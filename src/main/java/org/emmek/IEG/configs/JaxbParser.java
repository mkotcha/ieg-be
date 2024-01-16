package org.emmek.IEG.configs;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.helpers.xml.FlussoMisure;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.util.function.Function;

@Configuration
@Slf4j
public class JaxbParser {


    JAXBContext context = JAXBContext.newInstance(FlussoMisure.class);

    public JaxbParser() throws JAXBException {
    }

    @Bean
    public Function<String, FlussoMisure> parseXmlFunction() {
        return this::parseXml;
    }

    public FlussoMisure parseXml(String filename) {
        try {
            return (FlussoMisure) context.createUnmarshaller().unmarshal(new FileReader(filename));
        } catch (Exception e) {
            log.error("Errore nel parsing del file XML", e);
            return null;
        }
    }
    
}
