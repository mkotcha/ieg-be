package org.emmek.IEG.services;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.enums.TipoLettura;
import org.emmek.IEG.payloads.VerificaLettureDTO;
import org.emmek.IEG.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifica, prima di generare le fatture del mese, che ogni fornitura che verrà fatturata abbia le letture giuste:
 * stesso insieme di forniture della generazione massiva (tutte quelle dei clienti attivi) e stesso periodo del
 * calcolo della fattura (LetturaService.periodoLetture).
 */
@Service
public class VerificaLettureService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private LetturaService letturaService;

    @Transactional(readOnly = true)
    public List<VerificaLettureDTO> verifica(int mese, int anno) {
        List<VerificaLettureDTO> esiti = new ArrayList<>();
        for (Cliente cliente : clienteRepository.findByIsActiveTrueOrderById()) {
            if (cliente.getForniture() == null) continue;
            for (Fornitura fornitura : cliente.getForniture()) {
                esiti.add(verifica(cliente, fornitura, mese, anno));
            }
        }
        return esiti;
    }

    private VerificaLettureDTO verifica(Cliente cliente, Fornitura fornitura, int mese, int anno) {
        LetturaService.PeriodoLetture periodo = letturaService.periodoLetture(fornitura, mese, anno);
        // ordinate per data decrescente
        List<Lettura> letture = letturaService.lettureDelPeriodo(fornitura, periodo);
        Lettura ultima = letture.isEmpty() ? null : letture.get(0);
        Lettura prima = letture.isEmpty() ? null : letture.get(letture.size() - 1);

        List<String> problemi = new ArrayList<>();
        if (letture.size() < 2) {
            problemi.add(letture.isEmpty()
                    ? "nessuna lettura nel periodo: la fattura non si può calcolare"
                    : "una sola lettura nel periodo (" + DATA.format(prima.getDataLettura()) + "): la fattura non si può calcolare");
        } else {
            if (prima.getDataLettura().isAfter(periodo.dal())) {
                problemi.add(String.format("manca la lettura iniziale del %s (%s): la fattura parte dal %s, %d giorni non fatturati",
                        DATA.format(periodo.dal()), descrizione(periodo.origine()), DATA.format(prima.getDataLettura()),
                        ChronoUnit.DAYS.between(periodo.dal(), prima.getDataLettura())));
            }
            if (ultima.getDataLettura().isBefore(periodo.al())) {
                problemi.add(String.format("manca la lettura di fine mese (%s): la fattura si ferma al %s, %d giorni non fatturati",
                        DATA.format(periodo.al()), DATA.format(ultima.getDataLettura()),
                        ChronoUnit.DAYS.between(ultima.getDataLettura(), periodo.al())));
            }
            if (ultima.getTipoLettura() == TipoLettura.STIMA) {
                problemi.add("la lettura finale è stimata");
            }
        }
        return new VerificaLettureDTO(fornitura.getId(), cliente.getId(), cliente.getRagioneSociale(),
                periodo.dal(), periodo.al(), periodo.origine().name(), letture.size(),
                prima != null ? prima.getDataLettura() : null,
                ultima != null ? ultima.getDataLettura() : null,
                ultima != null && ultima.getTipoLettura() != null ? ultima.getTipoLettura().name() : null,
                letture.size() >= 2, problemi);
    }

    private static String descrizione(LetturaService.OrigineInizio origine) {
        return switch (origine) {
            case FATTURA_PRECEDENTE -> "lettura finale della fattura precedente";
            case LETTURA_SWITCH -> "lettura di switch";
            case FINE_MESE_PRECEDENTE -> "fine del mese precedente, prima fattura";
        };
    }
}
