package net.es.oscars.test.unit;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.nso.IntegerSet;
import net.es.oscars.nso.NsoResvException;
import net.es.oscars.nso.NsoSdpIdService;
import net.es.oscars.topo.beans.IntRange;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Category({UnitTests.class})
public class NsoResourceAllocationTest {
    @Test
    public void verifyIntegerSetOperations() {
        Set<Integer> usedSet = new HashSet<>();
        for (int i = 1; i <= 5; i++) {
            usedSet.add(i);
        }
        Set<Integer> allowedSet = new HashSet<>();
        for (int i = 3; i <= 9; i++) {
            allowedSet.add(i);
        }
        Set<Integer> availableIds = IntegerSet.availableFromUsedAndAllowedSets(usedSet, allowedSet);
        assert availableIds.contains(6);
        assert availableIds.contains(7);
        assert availableIds.contains(8);
        assert availableIds.contains(9);
        assert availableIds.size() == 4;

        Set<Integer> fromExpr = IntRange.singleSetFromExpr("1:5");
        log.info(fromExpr.toString());
        assert fromExpr.size() == 5;
        assert fromExpr.contains(1);
        assert fromExpr.contains(5);

        fromExpr = IntRange.singleSetFromExpr("1:5,10");
        assert fromExpr.size() == 6;
        assert fromExpr.contains(10);

        fromExpr = IntRange.singleSetFromExpr("1:2,10:11");
        assert fromExpr.size() == 4;

        availableIds = IntegerSet.availableFromRangeStrings("1:3", "1:5");
        assert availableIds.contains(4);
        assert availableIds.contains(5);
        assert availableIds.size() == 2;
    }
    @Test
    public void verifySdpOperations() throws NsoResvException {
        Set<Integer> availAtA = IntRange.singleSetFromExpr("2:100");
        Set<Integer> availAtZ = IntRange.singleSetFromExpr("90:100, 10, 13:15");

        Map<NsoVplsSdpPrecedence, Integer> sdpMap = NsoSdpIdService.getCommonUnusedSdpIds(availAtA, availAtZ, false);
        assert sdpMap.size() == 1;
        assert sdpMap.containsKey(NsoVplsSdpPrecedence.PRIMARY);
        assert sdpMap.get(NsoVplsSdpPrecedence.PRIMARY).equals(10);
        sdpMap = NsoSdpIdService.getCommonUnusedSdpIds(availAtA, availAtZ, true);
        assert sdpMap.size() == 2;
        assert sdpMap.containsKey(NsoVplsSdpPrecedence.PRIMARY);
        assert sdpMap.get(NsoVplsSdpPrecedence.PRIMARY).equals(13);
        assert sdpMap.containsKey(NsoVplsSdpPrecedence.SECONDARY);
        assert sdpMap.get(NsoVplsSdpPrecedence.SECONDARY).equals(14);


    }
}
