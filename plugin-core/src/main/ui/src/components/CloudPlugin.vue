<script setup lang="ts">
import { FeatherButton } from "@featherds/button";
import { FeatherTextarea } from '@featherds/textarea'
import { FeatherSnackbar } from '@featherds/snackbar'
import { FeatherDialog } from "@featherds/dialog";
import { onMounted, ref } from "vue";
import Toggle from './Toggle.vue'
import StatusBar from './StatusBar.vue';

const active = ref(false);
const activated = ref(false);
const toggle = () => active.value = !active.value
const status = ref({});
const notification = ref({});
const show = ref(false);
const key = ref('');
const keyDisabled = ref(false);
const visible = ref(false);


/**
 * Attempts to GET the Plugin Status from the Server.
 * Should notify on error, but silent on success (will just display the values)
 */
const updateStatus = async () => {
  const val = await fetch('/opennms/rest/plugin/cloud/config/status', { method: 'GET' })
  try {
    const jsonResponse = await val.json();
    status.value = jsonResponse;
    notification.value = jsonResponse;
    show.value = true;
  } catch (e) {
    notification.value = e as {};
    show.value = true;
  }
}

/**
 * Attempts to PUT the activation key over to the server.
 * Will notify on error, but otherwise is silent.
 */
const submitKey = async () => {
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

}

/**
 * Interim method until the API is ready. We can probably call submitKey directly when its ready.
 */
const tryToSubmit = () => {
  // console.log('Trying to submit the key', key, 'WHEN API IS READY, REMOVE THIS AND UNCOMMENT LINE BELOW')
  // notification.value = 'Fake API for now. Your key is:' + key.value;
  // show.value = true;
  submitKey();
}
const tryToDeactivate = () => {
  console.log('deactivating');
  //submitKey();
}

const cancel = () => {
  console.log('cancel');
  //route to previous page if router is available
};

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
    <h1>
      OpenNMS Cloud Services
      <StatusBar :status="status" />
    </h1>
  
    <p class="margin-bottom">Activate OpenNMS cloud-hosted services including 
      <a target="_blank" href="https://docs.opennms.com/horizon/latest/deployment/time-series-storage/timeseries/hosted-tss.html">time series storage</a>.
    </p>
    <Toggle :active="active" :toggle="toggle" activeText="Cloud Services Activated"
      disabledText="Cloud Services Deactivated" />
    <div class="key-entry" v-if="active && !activated">
      <p class="smaller">To activate, generate a key in the OpenNMS Portal and paste it here.</p>
      <div v-if="!activated">
        <FeatherTextarea :disabled="keyDisabled" label="Enter Activation Key" rows="5" :modelValue="key"
          @update:modelValue="(val: string) => key = val" />
        <FeatherButton text @click="cancel">Cancel</FeatherButton>
        <FeatherButton primary @click="tryToSubmit">Activate</FeatherButton>
      </div>
      <div v-if="activated">
        <FeatherButton text @click="cancel">Return to Dashboard</FeatherButton>
        <FeatherButton primary @click="tryToDeactivate">Deactivate Cloud Services</FeatherButton>
      </div>
    </div>
  </div>
  <FeatherDialog>
      <p class="my-content">A message from the Dialog</p>
      <template v-slot:footer>
        <FeatherButton primary @click="visible = false"
          >Close Dialog</FeatherButton
        >
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
</style>
