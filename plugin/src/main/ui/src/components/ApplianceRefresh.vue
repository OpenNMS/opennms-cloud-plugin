<template>
    <FeatherButton>
        Check for New Appliances
    </FeatherButton>
    <div>
      <table
        class="tc1 tr2 tc4 tr6"
        :class="{ condensed: condensed, striped: striped, hover: hover }"
      >
        <thead>
          <tr>
            <FeatherSortHeader
              scope="col"
              property="ApplianceName"
              :sort="sortAppliances"
              v-on:sort-changed="sortChanged"
            >
              Appliance Name
            </FeatherSortHeader>
            <th scope="col">Node Id</th>
            <th scope="col">IP Address</th>
            <th scope="col">Location</th>
            <th scope="col">Minion Status</th>
          </tr>
        </thead>
        <tbody>
          <tr :class="{ selected: selected }">
            <td>virtual-appliance-elara</td>
            <td>IP Address</td>
            <td>192.168.4.11</td>
            <td>Kanata_Office</td>
            <td>UP / UP</td>
          </tr>
        </tbody>
      </table>
      <FeatherPagination :total="100" :page-size="10" :modelValue="1" />
    </div>
</template>

<script setup lang="ts">
import { FeatherButton } from '@featherds/button';
import { FeatherPagination } from "@featherds/pagination";
import Face from "@featherds/icon/action/AccountCircle";
import { FeatherSortHeader, SORT } from "@featherds/table";

const props = defineProps({
      striped: false,
      condensed: false,
      hover: false,
      selected: false,
      icon: Face,
      all: false,
      checkbox1: true,
      checkbox2: false,
      sortTime: SORT.ASCENDING,
      sortDate: SORT.NONE,
      sortIcon: SORT.NONE,    
})
</script>

<style lang="scss" scoped>
@import "@featherds/table/scss/table";
table {
  width: 100%;
  @include table();
  @include row-select();
  &.hover {
    @include row-hover();
  }
  &.condensed {
    @include table-condensed();
  }
  &.striped {
    @include row-striped();
  }
}
</style>