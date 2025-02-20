package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.topo.beans.DevicePositions;
import net.es.oscars.topo.beans.MapNode;
import net.es.oscars.topo.beans.Position;
import net.es.oscars.topo.beans.Device;
import net.es.oscars.web.rest.MapController;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class MapPositionSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private MapController mapController;

    private List<MapNode> mapNodes = new ArrayList<>();
    private List<Device> devices = new ArrayList<>();
    private Map<String, Position> positionMap;

    @Given("^I load my positionMap from \"([^\"]*)\"$")
    public void my_position_map(String path) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            File jsonFile = SharedSteps.loadResource(path).getFile();
            positionMap = mapper.readValue(jsonFile, DevicePositions.class).getPositions();
        } catch (Exception ex) {
            world.add(ex);
        }
    }

    @Given("^I load devices from \"([^\"]*)\"$")
    public void my_devices(String path) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File jsonFile = SharedSteps.loadResource(path).getFile();
            devices = Arrays.asList(mapper.readValue(jsonFile, Device[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MapNode n1 = mapController.addNode(devices.get(0), positionMap);
        MapNode n2 = mapController.addNode(devices.get(1), positionMap);
        MapNode n3 = mapController.addNode(devices.get(2), positionMap);

        Collections.addAll(mapNodes, n1, n2, n3);
    }

    @Then("^I can verify my results$")
    public void verify() {
        MapNode n1 = mapNodes.get(0);
        MapNode n2 = mapNodes.get(1);
        MapNode n3 = mapNodes.get(2);

        Device a = devices.get(0);
        Device b = devices.get(1);
        Device c = devices.get(2);

        Integer n1_X = n1.getX();
        Integer n1_Y = n1.getY();
        Integer d1_X = positionMap.get(a.getUrn().split("-")[0]).getX();
        Integer d1_Y = positionMap.get(a.getUrn().split("-")[0]).getY();

        assert n1_X.equals(d1_X);
        assert n1_Y.equals(d1_Y);

        Integer n2_X = n2.getX();
        Integer n2_Y = n2.getY();
        Integer d2_X = positionMap.get(b.getUrn().split("-")[0]).getX();
        Integer d2_Y = positionMap.get(b.getUrn().split("-")[0]).getY();

        assert n2_X.equals(d2_X);
        assert n2_Y.equals(d2_Y);

        Integer n3_X = n3.getX();
        Integer n3_Y = n3.getY();
        Integer d3_X = positionMap.get(c.getUrn().split("-")[0]).getX();
        Integer d3_Y = positionMap.get(c.getUrn().split("-")[0]).getY();

        assert n3_X.equals(d3_X);
        assert n3_Y.equals(d3_Y);
    }
}