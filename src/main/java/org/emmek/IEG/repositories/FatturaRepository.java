package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.payloads.FatturaElencoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FatturaRepository extends JpaRepository<Fattura, Long> {
    List<Fattura> findByCliente(Cliente cliente);

    List<Fattura> findByMeseAndAnnoOrderByNumeroFattura(int mese, int anno);

    boolean existsByNumeroFattura(String numeroFattura);

    Optional<Fattura> findByNumeroFattura(String numeroFattura);

    /**
     * Elenco leggero di tutte le fatture, dalla più recente (mese di consumo) alla più vecchia.
     */
    @Query("""
            select new org.emmek.IEG.payloads.FatturaElencoDTO(f.numeroFattura, f.dataFattura, f.mese, f.anno, c.id,
                c.ragioneSociale, count(fs), f.consumoTot, f.totaleImponibile, f.totaleIva, sum(fs.totaleDaPagare))
            from Fattura f join f.cliente c left join f.fattureSingole fs
            group by f.id, c.id
            order by f.anno desc, f.mese desc, f.numeroFattura desc
            """)
    List<FatturaElencoDTO> findElenco();
}
