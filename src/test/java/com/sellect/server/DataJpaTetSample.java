package com.sellect.server;

import com.sellect.server.config.JsonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JsonConfig.class})
public class DataJpaTetSample {

    @Autowired
    private TestEntityManager em;

    @Test
    void contextLoads() {
    }

}
