package net.es.oscars.cuke;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Category({UnitTests.class})
public class NsoLspStateSyncerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    NsoVplsStateSyncer syncer;


}
