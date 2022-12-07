<script setup lang="ts">
import { FeatherButton } from "@featherds/button";
import { FeatherTextarea } from '@featherds/textarea'
import { FeatherSnackbar } from '@featherds/snackbar'
import { FeatherDialog } from "@featherds/dialog";
import { FeatherSpinner } from "@featherds/progress";
import { onMounted, ref } from "vue";
import StatusBar from './StatusBar.vue';

const status = ref('deactivated');
const notification = ref({});
const show = ref(false);
const key = ref('');
const keyError = ref('');
const keyDisabled = ref(false);
const loading = ref(false);
const displayDialog = ref(false);
const initialLoad = ref(false);
const labels = {
  title: 'Important',
  close: 'Close'
}


/**
 * Attempts to GET the Plugin Status from the Server.
 */
const updateStatus = async () => {
  loading.value = true;
  const val = await fetch('/opennms/rest/plugin/cloud/config/status', { method: 'GET' })
  try {
    const jsonResponse = await val.json();
    status.value = jsonResponse.status;
  } catch (e: any) {
    notification.value = e?.status || e;
    show.value = true;
  }
  loading.value = false;
  //We set this to avoid rendering issues before feather appears to be properly loaded.
  initialLoad.value = true;
}

const routeToHome = () => {
  window.location.href = window.location.origin + '/opennms';
}

/**
 * Attempts to PUT the activation key over to the server.
 * Will notify on error, but otherwise is silent.
 */
const submit = async (deactivate?: boolean) => {
  if(!deactivate && !key.value){
    keyError.value = "Key is Required.";
    return;
  } else if(keyError.value && key.value){
    keyError.value = "";
  }
  const prevStatus = status.value;
  loading.value = true;
  status.value = 'activating';
  const path = deactivate ? '/opennms/rest/plugin/cloud/config/deactivatekey' : '/opennms/rest/plugin/cloud/config/activationkey';
  const val = await fetch(path, { method: 'PUT', body: JSON.stringify({ key: key }) })
  try {
    const jsonResponse = await val.json();
    //{ "message": "UNAVAILABLE: Unable to resolve host access.production.prod.dataservice.opennms.com", "status": "FAILED" } 
    if(!deactivate)
      status.value = jsonResponse.status !== 'FAILED' ? 'CONFIGURED' : 'error';
    else {
      status.value = jsonResponse.status !== 'FAILED' ? 'deactivated' : 'error';
    }
    notification.value = jsonResponse.status;
    show.value = true;
    if (jsonResponse.success) {
        status.value = jsonResponse.status;
        show.value = true;
    }
  } catch (e: any) {
    status.value = prevStatus;
    notification.value = e?.status || e;
    show.value = true;
  }
  displayDialog.value = false;
  loading.value = false;
}


onMounted(async () => {
  // notification.value = 'Plugin Mounted. Faking API Status call';
  // show.value = true;
  // console.log('Trying to Load the Status. WHEN API IS READY, REMOVE THIS AND UNCOMMENT LINE BELOW')
  updateStatus();
})
</script>

<template>
  <FeatherSnackbar v-model="show">
    {{ notification }}
    <template v-slot:button>
      <FeatherButton @click="show = false" text>dismiss</FeatherButton>
    </template>
  </FeatherSnackbar>
  <div class="center">
    <div class = "header-breakout">
      <h1>
        OpenNMS Cloud Services
      </h1>
      <StatusBar v-if="initialLoad" :status="status" />
    </div>
    <div class="main-content">
      <h3 class="subheader">{{status === 'CONFIGURED' ? 'Manage' : 'Activate'}} OpenNMS Cloud-Hosted Services</h3>
      Activating cloud services enables the OpenNMS Time Series DB to store and persist performance metrics that OpenNMS collects to the cloud.
      <div  class="key-entry">
        <div 
          v-if="status !== 'CONFIGURED'"
          style="display: flex; flex-direction: row;"
        >
          <FeatherTextarea
            :disabled="keyDisabled"
            :error="keyError"
            style="width: 391px"
            label="Enter Activation Key"
            rows="5" 
            :modelValue="key"
            @update:modelValue="(val: string) => key = val" 
          />
          <div style="margin-left: 25px">
            You need an activation key to connect with OpenNMS cloud services. 
            <a href="https://portal.opennms.com" target="_blank">Log in to the OpenNMS Portal</a>
             to get this activation key, copy it, and then paste it into the field here.
          </div>
        </div>
        <div style="display: flex">
          <FeatherButton 
            id="cancel"
            :disabled="loading === true"
            text 
            @click="routeToHome"
          >
            {{ status === 'CONFIGURED' ? 'Return to Dashboard' : 'Cancel' }}
          </FeatherButton>
          <FeatherButton 
            id="activate" 
            v-if="status !== 'CONFIGURED'" 
            primary 
            @click="submit()"
          >
            {{loading ? 'Activating' : 'Activate Cloud Services'}}
          </FeatherButton>
          <FeatherButton 
            id="deactivate" 
            v-if="status === 'CONFIGURED'" 
            primary
            @click="displayDialog = true"
          >
            Deactivate Cloud Services
          </FeatherButton>
          <FeatherSpinner v-if="loading === true"/>
        </div>
      </div>
    </div>
  </div>
  <FeatherDialog
    v-model="displayDialog"
    :labels="labels"
  >
    <div style="width: 630px;">
      Deactivating cloud services requires you to perform two extra tasks:
      <ol type="1">
        <li>Remove <em>/opt/opennms/etc/opennms.properties.d/hosted_tsdb.timeseries.properties</em> from your OpenNMS system</li>.
        <li>Restart OpenNMS</li>.
      </ol>
      <strong>Caution:</strong> Once you deactivate cloud services and restart OpenNMS your data will persist to RRD storage unless otherwise configured.  You will <strong>no longer</strong> be able to access the data stored in the cloud or view the status of this system in the OpenNMS Portal.
    </div>      
    <template v-slot:footer>
      <FeatherButton text @click="displayDialog = false">
        Cancel
      </FeatherButton>
      <FeatherButton primary @click="submit(true)">
        Confirm Deactivation
      </FeatherButton>
      <FeatherSpinner v-if="loading"/>
    </template>
  </FeatherDialog>
</template>


<style scoped lang="scss">
@import "@featherds/styles/mixins/typography";
@import "@featherds/styles/themes/variables";

h1 {
  @include display1();
}

p {
  @include headline1();
  margin-bottom: 0;
}

p.small {
  @include headline2();
  margin-bottom: var($spacing-xxl);
}
p.smaller {
  @include headline3();
  margin-bottom: var($spacing-xxl);
}


.center {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.key-entry {
  margin-top: 40px;
}
.margin-bottom {
  margin-bottom:24px;
}

.main-content {
  //position: absolute;
  width: 696px;
  //height: 370px;
  //left: 648px;
  //top: 204px;
  background: #FFFFFF;
  box-shadow: inset 0px 0px 0px 1px rgba(0, 0, 0, 0.12);
  border-radius: 4px;
  padding: 15px;
  padding-bottom: 24px;
}

.header-breakout {
  display: flex;
  align-items: left;
  margin-bottom: 20px;
}

.subheader {
  color: var(--feather-primary);
  margin-bottom: 1rem;
}

.spinner-container {
  margin-left: 10px;
}
</style>
