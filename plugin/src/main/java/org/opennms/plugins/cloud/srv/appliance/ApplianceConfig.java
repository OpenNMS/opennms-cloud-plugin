/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.cloud.srv.appliance;

import java.util.ArrayList;
import java.util.List;

public class ApplianceConfig {
    private String name = new String(); // label of this appliance
    private String uuid = new String(); // cloud service ID of this appliance
    private int nodeId = 0; // Horizon node ID of this appliance
    private int minionId = 0; // Horizon node ID of the minion for this appliance
    private List<String> addresses = new ArrayList<>();

    public ApplianceConfig() {
    }

    public void addAddress(String address) {
        addresses.add(address);
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public boolean removeAddress(String address) {
        return addresses.remove(address);
    }

    public void clearAddresses() {
        addresses.clear();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getMinionId() {
        return minionId;
    }

    public void setMinionId(int minionId) {
        this.minionId = minionId;
    }
}
