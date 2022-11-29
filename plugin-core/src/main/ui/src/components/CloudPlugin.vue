<script setup lang="ts">
import { FeatherButton } from "@featherds/button";
import { FeatherTextarea } from '@featherds/textarea'
import { FeatherSnackbar } from '@featherds/snackbar'
import { FeatherDialog } from "@featherds/dialog";
import { onMounted, ref } from "vue";
import Toggle from './Toggle.vue'
import StatusBar from './StatusBar.vue';
import router from "../router";

const active = ref(false);
const activated = ref(false);
const toggle = () => active.value = !active.value
const status = ref('deactivated');
const notification = ref({});
const show = ref(false);
const key = ref('');
const keyDisabled = ref(false);
const visible = ref(false);
const loading = ref(false);
const displayDialog = ref(false);

const labels = {
  title: 'Important',
  close: 'Close'
}


/**
 * Attempts to GET the Plugin Status from the Server.
 * Should notify on error, but silent on success (will just display the values)
 */
const updateStatus = async () => {
  loading.value = true;
  console.log('loading initial');
  const val = await fetch('/opennms/rest/plugin/cloud/config/status', { method: 'GET' })
  try {
    const jsonResponse = await val.json();
    console.log('setting status to', jsonResponse)
    status.value = jsonResponse;
    notification.value = jsonResponse;
    show.value = true;
  } catch (e) {
    notification.value = e as {};
    show.value = true;
  }
  loading.value = false;
}

/**
 * Attempts to PUT the activation key over to the server.
 * Will notify on error, but otherwise is silent.
 */
const submitKey = async () => {
  status.value = 'activating';
  console.log('activating (submitKey)');
  loading.value = true;
  const val = await fetch('/opennms/rest/plugin/cloud/config/activationkey', { method: 'PUT', body: JSON.stringify({ key: key }) })
  try {
    const jsonResponse = await val.json();
    status.value = jsonResponse;
    notification.value = jsonResponse;
    show.value = true;
    if (jsonResponse.success) {
        // do something
    }
  } catch (e) {
    notification.value = e as {};
    show.value = true;
  }
  loading.value = false;

}

/**
 * Interim method until the API is ready. We can probably call submitKey directly when its ready.
 */
const tryToSubmit = () => {
  // console.log('Trying to submit the key', key, 'WHEN API IS READY, REMOVE THIS AND UNCOMMENT LINE BELOW')
  // notification.value = 'Fake API for now. Your key is:' + key.value;
  // show.value = true;
  

  //submitKey();
  loading.value = true;
  status.value = 'activating';
  console.log('activating (tryToSubmit)');
  //testing switchover
  setTimeout(() => { status.value = 'activated';}, 5000); 
  
  displayDialog.value = true;
}

const showDeactivationDialog = () => {
  displayDialog.value = true;
};

const tryToDeactivate = () => {
  console.log('deactivating');
  visible.value = false;
  //submitKey();
  loading.value = true;
  setTimeout(() => {status.value = 'deactivated', 5000});
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
      <StatusBar :status="status" />
    </div>
    <div class="main-content">
      <h3 class="subheader">{{activated ? 'Manage' : 'Activate'}} OpenNMS Cloud-Hosted Services</h3>
      Activating cloud services enables the OpenNMS Time Series DB to store and persist performance metrics that OpenNMS collects to the cloud.
      <div  class="key-entry">
        <div style="display: flex; flex-direction: row;">
          <FeatherTextarea 
            v-if="!activated"
            :disabled="keyDisabled"
            style="width: 391px"
            label="Enter Activation Key"
            rows="5" 
            :modelValue="key"
            @update:modelValue="(val: string) => key = val" 
          />
          <div style="margin-left: 25px">
            You need an activation key to connect with OpenNMS cloud services. <a href="https://portal.opennms.com" target="_blank">Log in to the OpenNMS Portal</a> to get this activation key, copy it, and then paste it into the field here.
          </div>
        </div>
        <FeatherButton id="cancel" text href="/">{{ activated ? 'Return to Dashboard' : 'Cancel' }}</FeatherButton>
        <FeatherButton 
          id="activate" 
          v-if="!activated" 
          primary 
          @click="tryToSubmit"
        >
          {{loading ? 'Activating' : 'Activate Cloud Services'}}
        </FeatherButton>
        <FeatherButton 
          id="deactivate" 
          v-if="activated" 
          primary
          @click="showDeactivationDialog"
        >
          Deactivate Cloud Services
        </FeatherButton>
      </div>
    </div>
  </div>
  <FeatherDialog style="width: 688px" v-model="displayDialog" :labels="labels">
      <div>
        Deactivating cloud services requires you to restart OpenNMS. After deactivation and restart, your data will persist to RRD storage. You will no longer be able to access the data stored in the cloud or view it in the Portal.
      </div>      
      <template v-slot:footer>
        <FeatherButton text @click="displayDialog = false">
          Cancel
        </FeatherButton>
        <FeatherButton primary @click="tryToDeactivate">
          Confirm Deactivation
        </FeatherButton>
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
</style>
