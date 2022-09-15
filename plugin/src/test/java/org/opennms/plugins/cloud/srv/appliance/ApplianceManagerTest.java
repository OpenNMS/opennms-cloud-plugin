package org.opennms.plugins.cloud.srv.appliance;

import org.junit.Test;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.OnmsBrokerActiveMq;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.OnmsHttpInfo;

public class ApplianceManagerTest {

//    @Test
    public void test() throws Exception {
        var instance = new ApplianceManager(null, null);

//        var result = instance.listAppliances();
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
