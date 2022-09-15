package org.opennms.plugins.cloud.srv.appliance;

import org.junit.Test;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.OnmsBrokerActiveMq;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.OnmsHttpInfo;

import java.util.Objects;
import java.util.stream.Collectors;

public class ApplianceManagerTest {

//    @Test
    public void demoTime() {
        var instance = new ApplianceManager(null, null);

        // We expect the user to provide the plugin with the UUID of the instance.
        var instanceId = "330612a0-392a-46f9-b679-b3e96c8ba8b8"; // TODO: fill in

        // Cool, so we have an instance... Does this instance have an associated connectivity profile?
        // Simplifying assumption: we're picking up the first one we see.
        var connProfId = instance.getConnectivityProfileIdByInstanceId(instanceId);

        // In this scenario, we don't have a connectivity profile - so we need to create one.
        if (connProfId == null) {
            var httpInfo = new OnmsHttpInfo();
            httpInfo.setHttpUrl("http://20.110.207.236:8980/opennms");
            httpInfo.setHttpUser("admin");
            httpInfo.setHttpPassword("OpenUlf123!!");

            var broker = new OnmsBrokerActiveMq();
            broker.setUrl("tcp://20.110.207.236:61616");
            broker.setUser("admin");
            broker.setPassword("OpenUlf123!!");

            // Simulate Platform Access Service - create the connectivity profile.
            instance.createConnectivityProfile(instanceId, httpInfo, broker);

            connProfId = instance.getConnectivityProfileIdByInstanceId(instanceId);
        }

        // And now, we work on setting up locations under the appliance
        var appliances = instance.listAppliances()
                .stream()
                .filter(appliance -> Objects.equals(appliance.getId(), "e71956c3-cca8-4119-8e26-a38a92cbf9aa"))
                .collect(Collectors.toList());

        var locations = instance.listLocations();

        // This variable is needed since Java kinda sucks.
        String finalConnProfId = connProfId;

        appliances.forEach(appliance -> {
                    var candidateLocation = locations
                            .stream()
                            .filter(ln -> ln.getName().contains("kiwi-den-" + appliance.getLabel()))
                            .collect(Collectors.toList())
                            .stream()
                            .findFirst();

                    String locationId;
                    if (candidateLocation.isPresent()) {
                        locationId = candidateLocation.get().getId();
                    } else {
                        locationId = instance.createLocation("kiwi-den-" + appliance.getLabel(), instanceId, finalConnProfId);
                    }

                    instance.setApplianceMonitoringWorkload(appliance.getId(), locationId);
                }
        );

    }

//    @Test
    public void test() throws Exception {
        var instance = new ApplianceManager(null, null);

        var appliances = instance.listAppliances();
//        System.out.println("Appliances: " + result);

//        var info = instance.getApplianceInfo("e71956c3-cca8-4119-8e26-a38a92cbf9aa");
//        System.out.println("Appliance info: " + info);

//        var states = instance.getApplianceStates("777844e9-090c-4dff-8f20-487f065c83b2");
//        System.out.println("Appliance states: " + states);

        // At ths is point, the user has already created their OpenNMS instance (managed) in the UI.
        var instanceId = instance.getInstanceIdByName("Bowser");
        if (instanceId == null) {
            throw new IllegalStateException("Unable to find Onms instance");
        }

        var httpInfo = new OnmsHttpInfo();
        httpInfo.setHttpUrl("http://20.110.207.236:8980/opennms");
        httpInfo.setHttpUser("admin");
        httpInfo.setHttpPassword("OpenUlf123!!");

        var broker = new OnmsBrokerActiveMq();
        broker.setUrl("tcp://20.110.207.236:61616");
        broker.setUser("admin");
        broker.setPassword("OpenUlf123!!");

        // Simulate Platform Access Service - create the connectivity profile.
        instance.createConnectivityProfile(instanceId, httpInfo, broker);

        var connProfId = instance.getConnectivityProfileIdByInstanceId(instanceId);
        if (connProfId == null) {
            throw new IllegalStateException("Unable to get connectivity profile");
        }

        var locationId= instance.createLocation("kiwis-den", instanceId, connProfId);
        if (locationId == null) {
            throw new IllegalStateException("Unable to create location");
        }

        System.out.println("Instance Id: " + instanceId);
        System.out.println("Connectivity Profile Id: " + connProfId);
        System.out.println("Location Id: " + locationId);

        var applianceId = "e71956c3-cca8-4119-8e26-a38a92cbf9aa"; // Pierre's virtual appliance

        instance.setApplianceMonitoringWorkload(applianceId, locationId);
    }
}
