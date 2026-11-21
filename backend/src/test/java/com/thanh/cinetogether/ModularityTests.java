package com.thanh.cinetogether;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void moduleBoundariesAreRespectedAndHaveNoCycles() {
        ApplicationModules.of(CinetogetherApplication.class).verify();
    }

}
