<template>
  <AppPage :eyebrow="locale.t('page.bills.finance')" :title="locale.t('page.bills.title')" :stats="stats">
    <template #actions>
      <el-date-picker v-if="activeTab === 'bills' && user.hasPermission('bill:confirm')" v-model="generateMonth" type="month" value-format="YYYY-MM" :placeholder="locale.t('page.bills.generateMonth')" />
      <el-button v-if="activeTab === 'bills' && user.hasPermission('bill:confirm')" type="primary" :loading="generating" @click="handleGenerate">{{ locale.t('page.bills.generateDemo') }}</el-button>
    </template>

    <el-tabs v-model="activeTab" class="finance-tabs" @tab-change="handleTabChange">
      <el-tab-pane :label="locale.t('page.bills.monthlyBills')" name="bills">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.bills.billMonth')">
        <el-date-picker v-model="query.billMonth" type="month" value-format="YYYY-MM" :placeholder="locale.t('page.bills.monthPlaceholder')" />
      </el-form-item>
      <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.bills.advertiserId')">
        <el-select v-model="query.advertiserId" clearable filterable :loading="advertiserLoading" :placeholder="locale.t('page.bills.advertiserPlaceholder')" style="width: 220px">
          <el-option v-for="advertiser in advertiserOptions" :key="advertiser.id" :label="getAdvertiserLabel(advertiser)" :value="advertiser.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.bills.agentId')">
        <el-select v-model="query.agentId" clearable filterable :loading="agentLoading" :placeholder="locale.t('page.bills.agentPlaceholder')" style="width: 220px">
          <el-option v-for="agent in agentOptions" :key="agent.id" :label="getAgentLabel(agent)" :value="agent.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.bills.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.bills.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in billStatusMap" :key="key" :label="getStatusLabel('bill', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadBills">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="billNo" :label="locale.t('page.bills.billNo')" min-width="190" />
      <el-table-column :label="locale.t('page.bills.billType')" width="120">
        <template #default="{ row }">{{ getStringStatusLabel('billType', row.billType, billTypeMap[row.billType] || row.billType) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.bills.subject')" min-width="190">
        <template #default="{ row }">
          <div class="entity-cell">
            <strong>{{ getBillSubjectName(row) }}</strong>
            <span>{{ getBillSubjectCode(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="billMonth" :label="locale.t('page.bills.month')" width="110" />
      <el-table-column :label="locale.t('page.bills.amountTotal')" width="130">
        <template #default="{ row }">¥{{ formatMoney(row.amountTotal) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.bills.amountPaid')" width="130">
        <template #default="{ row }">¥{{ formatMoney(row.amountPaid) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.bills.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="billStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('bill', row.status, billStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confirmTime" :label="locale.t('page.bills.confirmTime')" min-width="170" />
      <el-table-column prop="payTime" :label="locale.t('page.bills.payTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="205" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetails(row)">{{ locale.t('page.bills.details') }}</el-button>
          <el-button v-if="user.hasPermission('bill:confirm') && row.status === 'pending'" link type="success" @click="handleConfirm(row.id)">{{ locale.t('page.bills.confirm') }}</el-button>
          <el-button v-if="user.hasPermission('bill:pay') && row.status !== 'paid'" link type="warning" @click="openPayDialog(row)">{{ locale.t('page.bills.markPaid') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.bills.empty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadBills"
        @size-change="loadBills"
      />
    </div>

      </el-tab-pane>
      <el-tab-pane v-if="canViewWalletAccounts" :label="locale.t('page.wallet.accounts')" name="accounts">
        <el-form class="filter-form" :model="walletQuery" inline>
          <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.wallet.ownerType')">
            <el-select v-model="walletQuery.ownerType" clearable :placeholder="locale.t('page.wallet.allOwnerTypes')" style="width: 150px">
              <el-option :label="locale.t('page.wallet.advertiser')" value="advertiser" />
              <el-option :label="locale.t('page.wallet.agent')" value="agent" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadWalletAccounts">{{ locale.t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="resetWalletQuery">{{ locale.t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="walletLoading" :data="walletAccounts" class="data-table" row-key="id">
          <el-table-column prop="accountNo" :label="locale.t('page.wallet.accountNo')" min-width="180" />
          <el-table-column :label="locale.t('page.wallet.owner')" min-width="200">
            <template #default="{ row }">
              <div class="entity-cell">
                <strong>{{ getWalletOwnerName(row.ownerType, row.ownerId) }}</strong>
                <span>{{ getWalletOwnerCode(row.ownerType, row.ownerId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.availableBalance')" width="140">
            <template #default="{ row }">¥{{ formatMoney(row.availableBalance) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.totalRecharge')" width="140">
            <template #default="{ row }">¥{{ formatMoney(row.totalRecharge) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.totalConsumed')" width="140">
            <template #default="{ row }">¥{{ formatMoney(row.totalConsumed) }}</template>
          </el-table-column>
          <el-table-column prop="status" :label="locale.t('page.bills.status')" width="100" />
          <template #empty>
            <el-empty :description="locale.t('page.wallet.accountEmpty')" />
          </template>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="canViewRecharges" :label="locale.t('page.wallet.recharges')" name="recharges">
        <el-form class="filter-form" :model="rechargeQuery" inline>
          <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.wallet.ownerType')">
            <el-select v-model="rechargeQuery.ownerType" clearable :placeholder="locale.t('page.wallet.allOwnerTypes')" style="width: 150px">
              <el-option :label="locale.t('page.wallet.advertiser')" value="advertiser" />
              <el-option :label="locale.t('page.wallet.agent')" value="agent" />
            </el-select>
          </el-form-item>
          <el-form-item :label="locale.t('page.bills.status')">
            <el-select v-model="rechargeQuery.status" clearable :placeholder="locale.t('page.bills.allStatus')" style="width: 150px">
              <el-option :label="locale.t('status.recharge.auditing')" value="auditing" />
              <el-option :label="locale.t('status.recharge.pending')" value="pending" />
              <el-option :label="locale.t('status.recharge.paid')" value="paid" />
              <el-option :label="locale.t('status.recharge.rejected')" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadRecharges">{{ locale.t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="resetRechargeQuery">{{ locale.t('common.reset') }}</el-button>
          </el-form-item>
          <el-form-item>
            <el-button v-if="canCreateRecharge" type="success" @click="openRechargeDialog">{{ locale.t('page.wallet.createRecharge') }}</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="rechargeLoading" :data="recharges" class="data-table" row-key="id">
          <el-table-column prop="rechargeNo" :label="locale.t('page.wallet.rechargeNo')" min-width="190" />
          <el-table-column :label="locale.t('page.wallet.owner')" min-width="200">
            <template #default="{ row }">
              <div class="entity-cell">
                <strong>{{ getWalletOwnerName(row.ownerType, row.ownerId) }}</strong>
                <span>{{ getWalletOwnerCode(row.ownerType, row.ownerId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.payMethod')" width="130">
            <template #default="{ row }">{{ locale.t(`status.payMethod.${row.payMethod}`, row.payMethod) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.amount')" width="130">
            <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.bills.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="getRechargeStatusType(row.status)" effect="dark">
                {{ locale.t(`status.recharge.${row.status}`, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="voucherNo" :label="locale.t('page.bills.paymentVoucherNo')" min-width="150" />
          <el-table-column prop="auditTime" :label="locale.t('page.wallet.auditTime')" min-width="170" />
          <el-table-column :label="locale.t('common.operation')" width="230" fixed="right" class-name="operation-column">
            <template #default="{ row }">
              <el-button v-if="canAuditRecharge && row.status === 'auditing'" link type="success" @click="handleConfirmRecharge(row)">{{ locale.t('page.wallet.confirmRecharge') }}</el-button>
              <el-button v-if="row.qrCodeUrl && row.status === 'pending'" link type="primary" @click="openScanPayDialog(row)">{{ locale.t('page.wallet.viewPayQr') }}</el-button>
              <el-link v-if="row.voucherUrl" type="primary" :href="row.voucherUrl" target="_blank">{{ locale.t('page.bills.viewVoucher') }}</el-link>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="locale.t('page.wallet.rechargeEmpty')" />
          </template>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="canViewRefunds" :label="locale.t('page.wallet.refunds')" name="refunds">
        <el-form class="filter-form" :model="refundQuery" inline>
          <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.wallet.ownerType')">
            <el-select v-model="refundQuery.ownerType" clearable :placeholder="locale.t('page.wallet.allOwnerTypes')" style="width: 150px">
              <el-option :label="locale.t('page.wallet.advertiser')" value="advertiser" />
              <el-option :label="locale.t('page.wallet.agent')" value="agent" />
            </el-select>
          </el-form-item>
          <el-form-item :label="locale.t('page.bills.status')">
            <el-select v-model="refundQuery.status" clearable :placeholder="locale.t('page.bills.allStatus')" style="width: 150px">
              <el-option :label="locale.t('status.refund.auditing')" value="auditing" />
              <el-option :label="locale.t('status.refund.processing')" value="processing" />
              <el-option :label="locale.t('status.refund.refunded')" value="refunded" />
              <el-option :label="locale.t('status.refund.rejected')" value="rejected" />
              <el-option :label="locale.t('status.refund.failed')" value="failed" />
            </el-select>
          </el-form-item>
          <el-form-item :label="locale.t('page.wallet.reconcileStatus')">
            <el-select v-model="refundQuery.reconcileStatus" clearable :placeholder="locale.t('page.wallet.allReconcileStatus')" style="width: 150px">
              <el-option :label="locale.t('status.reconcile.pending')" value="pending" />
              <el-option :label="locale.t('status.reconcile.matched')" value="matched" />
              <el-option :label="locale.t('status.reconcile.exception')" value="exception" />
            </el-select>
          </el-form-item>          <el-form-item :label="locale.t('page.wallet.refundNo')">
            <el-input v-model.trim="refundQuery.refundNo" clearable :placeholder="locale.t('page.wallet.refundNo')" style="width: 210px" />
          </el-form-item>
          <el-form-item :label="locale.t('page.wallet.rechargeNo')">
            <el-input v-model.trim="refundQuery.rechargeNo" clearable :placeholder="locale.t('page.wallet.rechargeNo')" style="width: 210px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadRefunds">{{ locale.t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="resetRefundQuery">{{ locale.t('common.reset') }}</el-button>
          </el-form-item>
          <el-form-item>
            <el-button v-if="canCreateRefund" type="success" @click="openRefundDialog">{{ locale.t('page.wallet.createRefund') }}</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="refundLoading" :data="refunds" class="data-table" row-key="id">
          <el-table-column prop="refundNo" :label="locale.t('page.wallet.refundNo')" min-width="190" />
          <el-table-column prop="rechargeNo" :label="locale.t('page.wallet.rechargeNo')" min-width="190" />
          <el-table-column :label="locale.t('page.wallet.owner')" min-width="200">
            <template #default="{ row }">
              <div class="entity-cell">
                <strong>{{ getWalletOwnerName(row.ownerType, row.ownerId) }}</strong>
                <span>{{ getWalletOwnerCode(row.ownerType, row.ownerId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.payMethod')" width="130">
            <template #default="{ row }">{{ locale.t(`status.payMethod.${row.payMethod}`, row.payMethod) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.refundAmount')" width="130">
            <template #default="{ row }">¥{{ formatMoney(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.bills.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="getRefundStatusType(row.status)" effect="dark">
                {{ locale.t(`status.refund.${row.status}`, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.reconcileStatus')" width="120">
            <template #default="{ row }">
              <el-tag :type="getReconcileStatusType(row.reconcileStatus)" effect="plain">
                {{ locale.t(`status.reconcile.${row.reconcileStatus || 'pending'}`, row.reconcileStatus || 'pending') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="gatewayRefundStatus" :label="locale.t('page.wallet.gatewayRefundStatus')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="lastSyncTime" :label="locale.t('page.wallet.lastSyncTime')" min-width="170" />
          <el-table-column prop="refundReason" :label="locale.t('page.wallet.refundReason')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="refundTransactionNo" :label="locale.t('page.wallet.refundTransactionNo')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="auditTime" :label="locale.t('page.wallet.auditTime')" min-width="170" />
          <el-table-column :label="locale.t('common.operation')" width="220" fixed="right" class-name="operation-column">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRefundDetail(row)">{{ locale.t('page.bills.details') }}</el-button>
              <el-button v-if="canAuditRefund && ['processing', 'refunded', 'failed'].includes(row.status)" link type="warning" :loading="refundSyncingId === row.id" @click="handleSyncRefund(row)">{{ locale.t('page.wallet.syncRefundStatus') }}</el-button>
              <el-button v-if="canAuditRefund && (row.status === 'auditing' || row.status === 'failed')" link type="success" @click="openRefundAudit(row, 'approve')">{{ locale.t('page.wallet.approveRefund') }}</el-button>
              <el-button v-if="canAuditRefund && row.status === 'auditing'" link type="danger" @click="openRefundAudit(row, 'reject')">{{ locale.t('page.wallet.rejectRefund') }}</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="locale.t('page.wallet.refundEmpty')" />
          </template>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="canViewPayCallbacks" :label="locale.t('page.wallet.payCallbacks')" name="callbacks">
        <el-form class="filter-form" :model="callbackQuery" inline>
          <el-form-item :label="locale.t('page.wallet.payMethod')">
            <el-select v-model="callbackQuery.payMethod" clearable :placeholder="locale.t('page.wallet.allPayMethods')" style="width: 150px">
              <el-option :label="locale.t('status.payMethod.wechat')" value="wechat" />
              <el-option :label="locale.t('status.payMethod.alipay')" value="alipay" />
            </el-select>
          </el-form-item>
          <el-form-item :label="locale.t('page.wallet.callbackStatus')">
            <el-select v-model="callbackQuery.callbackStatus" clearable :placeholder="locale.t('page.bills.allStatus')" style="width: 160px">
              <el-option :label="locale.t('status.payCallback.success')" value="success" />
              <el-option :label="locale.t('status.payCallback.duplicate')" value="duplicate" />
              <el-option :label="locale.t('status.payCallback.ignored')" value="ignored" />
              <el-option :label="locale.t('status.payCallback.failed')" value="failed" />
            </el-select>
          </el-form-item>
          <el-form-item :label="locale.t('page.wallet.rechargeNo')">
            <el-input v-model.trim="callbackQuery.rechargeNo" clearable :placeholder="locale.t('page.wallet.rechargeNo')" style="width: 210px" />
          </el-form-item>
          <el-form-item :label="locale.t('page.wallet.transactionNo')">
            <el-input v-model.trim="callbackQuery.transactionNo" clearable :placeholder="locale.t('page.wallet.transactionNo')" style="width: 230px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadPayCallbackLogs">{{ locale.t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="resetCallbackQuery">{{ locale.t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="callbackLoading" :data="payCallbackLogs" class="data-table" row-key="id">
          <el-table-column prop="createTime" :label="locale.t('page.wallet.callbackTime')" min-width="170" />
          <el-table-column :label="locale.t('page.wallet.payMethod')" width="120">
            <template #default="{ row }">{{ locale.t(`status.payMethod.${row.payMethod}`, row.payMethod) }}</template>
          </el-table-column>
          <el-table-column prop="rechargeNo" :label="locale.t('page.wallet.rechargeNo')" min-width="190" />
          <el-table-column prop="transactionNo" :label="locale.t('page.wallet.transactionNo')" min-width="210" show-overflow-tooltip />
          <el-table-column :label="locale.t('page.wallet.callbackStatus')" width="120">
            <template #default="{ row }">
              <el-tag :type="getCallbackStatusType(row.callbackStatus)" effect="dark">
                {{ locale.t(`status.payCallback.${row.callbackStatus}`, row.callbackStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="tradeStatus" :label="locale.t('page.wallet.tradeStatus')" width="140" />
          <el-table-column :label="locale.t('page.wallet.paidAmount')" width="120">
            <template #default="{ row }">{{ row.paidAmount == null ? '-' : `¥${formatMoney(row.paidAmount)}` }}</template>
          </el-table-column>
          <el-table-column prop="failReason" :label="locale.t('page.wallet.failReason')" min-width="220" show-overflow-tooltip />
          <el-table-column prop="processedTime" :label="locale.t('page.wallet.processedTime')" min-width="170" />
          <template #empty>
            <el-empty :description="locale.t('page.wallet.callbackEmpty')" />
          </template>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="canViewCharges" :label="locale.t('page.wallet.playCharges')" name="charges">
        <el-form class="filter-form" :model="chargeQuery" inline>
          <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.wallet.ownerType')">
            <el-select v-model="chargeQuery.ownerType" clearable :placeholder="locale.t('page.wallet.allOwnerTypes')" style="width: 150px">
              <el-option :label="locale.t('page.wallet.advertiser')" value="advertiser" />
              <el-option :label="locale.t('page.wallet.agent')" value="agent" />
            </el-select>
          </el-form-item>
          <el-form-item :label="locale.t('page.bills.status')">
            <el-select v-model="chargeQuery.status" clearable :placeholder="locale.t('page.bills.allStatus')" style="width: 150px">
              <el-option :label="locale.t('status.playCharge.success')" value="success" />
              <el-option :label="locale.t('status.playCharge.failed')" value="failed" />
              <el-option :label="locale.t('status.playCharge.skipped')" value="skipped" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadPlayCharges">{{ locale.t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="resetChargeQuery">{{ locale.t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="chargeLoading" :data="playCharges" class="data-table" row-key="id">
          <el-table-column prop="chargeNo" :label="locale.t('page.wallet.chargeNo')" min-width="190" />
          <el-table-column :label="locale.t('page.wallet.owner')" min-width="200">
            <template #default="{ row }">
              <div class="entity-cell">
                <strong>{{ getWalletOwnerName(row.ownerType, row.ownerId) }}</strong>
                <span>{{ getWalletOwnerCode(row.ownerType, row.ownerId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.reports.adId')" min-width="170">
            <template #default="{ row }">{{ getAdName(row.adId) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.reports.planId')" min-width="170">
            <template #default="{ row }">{{ getPlanName(row.planId) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.quantity')" width="110">
            <template #default="{ row }">{{ formatMoney(row.quantity) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.wallet.chargeAmount')" width="130">
            <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.bills.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="getChargeStatusType(row.status)" effect="dark">
                {{ locale.t(`status.playCharge.${row.status}`, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="failReason" :label="locale.t('page.wallet.failReason')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" :label="locale.t('page.reports.createTime')" min-width="170" />
          <template #empty>
            <el-empty :description="locale.t('page.wallet.chargeEmpty')" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" :title="locale.t('page.bills.drawerTitle')" size="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.bills.billNo')">{{ current.billNo }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.subject')">{{ getBillSubjectName(current) }} / {{ getBillSubjectCode(current) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.billMonth')">{{ current.billMonth }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.amountTotal')">¥{{ formatMoney(current.amountTotal) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.paymentVoucherNo')">{{ current.paymentVoucherNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.paymentVoucherUrl')">
          <el-link v-if="current.paymentVoucherUrl" type="primary" :href="current.paymentVoucherUrl" target="_blank">
            {{ locale.t('page.bills.viewVoucher') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="details" class="data-table detail-table" row-key="id">
        <el-table-column prop="itemName" :label="locale.t('page.bills.itemName')" min-width="180" />
        <el-table-column :label="locale.t('page.reports.adId')" min-width="170">
          <template #default="{ row }">
            <div class="entity-cell">
              <strong>{{ getAdName(row.adId) }}</strong>
              <span>{{ getAdCode(row.adId) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="locale.t('page.reports.planId')" min-width="170">
          <template #default="{ row }">
            <div class="entity-cell">
              <strong>{{ getPlanName(row.planId) }}</strong>
              <span>{{ getPlanCode(row.planId) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="itemCount" :label="locale.t('page.reports.playCount')" width="100" />
        <el-table-column :label="locale.t('page.bills.amount')" width="110">
          <template #default="{ row }">¥{{ formatMoney(row.itemAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="payDialogVisible" :title="locale.t('page.bills.payConfirmTitle')" width="520px" destroy-on-close>
      <el-form ref="payFormRef" :model="payForm" :rules="payRules" label-width="120px">
        <el-form-item :label="locale.t('page.bills.paymentVoucherNo')" prop="paymentVoucherNo">
          <el-input v-model.trim="payForm.paymentVoucherNo" :placeholder="locale.t('page.bills.paymentVoucherNoPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.bills.paymentVoucherUrl')" prop="paymentVoucherUrl">
          <div class="voucher-upload">
            <el-upload
              :show-file-list="false"
              :http-request="handleVoucherUpload"
              :before-upload="beforeVoucherUpload"
            >
              <el-button :loading="voucherUploading">{{ locale.t('page.bills.uploadVoucher') }}</el-button>
            </el-upload>
            <el-input v-model="payForm.paymentVoucherUrl" :placeholder="locale.t('page.bills.paymentVoucherUrlPlaceholder')" />
            <el-image
              v-if="payForm.paymentVoucherUrl"
              class="voucher-preview"
              :src="payForm.paymentVoucherUrl"
              :preview-src-list="[payForm.paymentVoucherUrl]"
              fit="cover"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">{{ locale.t('page.bills.markPaid') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rechargeDialogVisible" :title="locale.t('page.wallet.createRecharge')" width="560px" destroy-on-close>
      <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeRules" label-width="120px">
        <el-alert v-if="!user.isPlatformScope && isBankRecharge" class="scope-alert" :title="locale.t('page.wallet.businessRechargeHint')" type="info" show-icon :closable="false" />
        <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.wallet.ownerType')" prop="ownerType">
          <el-radio-group v-model="rechargeForm.ownerType">
            <el-radio-button label="advertiser">{{ locale.t('page.wallet.advertiser') }}</el-radio-button>
            <el-radio-button label="agent">{{ locale.t('page.wallet.agent') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.wallet.owner')" prop="ownerId">
          <el-select v-model="rechargeForm.ownerId" filterable class="wide-control" :placeholder="locale.t('page.wallet.ownerPlaceholder')">
            <el-option v-for="item in rechargeOwnerOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else :label="locale.t('page.wallet.owner')">
          <span>{{ currentBusinessOwnerText }}</span>
        </el-form-item>
        <el-form-item :label="locale.t('page.wallet.amount')" prop="amount">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" class="wide-control" />
        </el-form-item>
        <el-form-item :label="locale.t('page.wallet.payMethod')" prop="payMethod">
          <el-radio-group v-model="rechargeForm.payMethod">
            <el-radio-button label="bank_transfer">{{ locale.t('status.payMethod.bank_transfer') }}</el-radio-button>
            <el-radio-button label="wechat">{{ locale.t('status.payMethod.wechat') }}</el-radio-button>
            <el-radio-button label="alipay">{{ locale.t('status.payMethod.alipay') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="isBankRecharge" :label="locale.t('page.bills.paymentVoucherNo')" prop="voucherNo">
          <el-input v-model.trim="rechargeForm.voucherNo" :placeholder="locale.t('page.bills.paymentVoucherNoPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="isBankRecharge" :label="locale.t('page.bills.paymentVoucherUrl')" prop="voucherUrl">
          <div class="voucher-upload">
            <el-upload :show-file-list="false" :http-request="handleRechargeVoucherUpload" :before-upload="beforeVoucherUpload">
              <el-button :loading="rechargeVoucherUploading">{{ locale.t('page.bills.uploadVoucher') }}</el-button>
            </el-upload>
            <el-input v-model="rechargeForm.voucherUrl" :placeholder="locale.t('page.bills.paymentVoucherUrlPlaceholder')" />
            <el-image v-if="rechargeForm.voucherUrl" class="voucher-preview" :src="rechargeForm.voucherUrl" :preview-src-list="[rechargeForm.voucherUrl]" fit="cover" />
          </div>
        </el-form-item>
        <el-form-item :label="locale.t('page.partners.remark')">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="3" :placeholder="locale.t('page.partners.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="rechargeSaving" @click="handleCreateRecharge">{{ isBankRecharge ? locale.t('common.save') : locale.t('page.wallet.createPayQr') }}</el-button>
      </template>
    </el-dialog>


    <el-dialog v-model="refundDialogVisible" :title="locale.t('page.wallet.createRefund')" width="560px" destroy-on-close>
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="120px">
        <el-alert class="scope-alert" :title="locale.t('page.wallet.refundHint')" type="info" show-icon :closable="false" />
        <el-form-item :label="locale.t('page.wallet.rechargeOrder')" prop="rechargeOrderId">
          <el-select v-model="refundForm.rechargeOrderId" filterable class="wide-control" :loading="rechargeLoading" :placeholder="locale.t('page.wallet.rechargeOrderPlaceholder')" @change="onRefundRechargeChange">
            <el-option v-for="item in refundableRecharges" :key="item.id" :label="getRechargeOptionLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-alert v-if="!rechargeLoading && refundableRecharges.length === 0" class="scope-alert" :title="locale.t('page.wallet.noRefundableOrders')" type="warning" show-icon :closable="false" />
        <div v-if="selectedRefundRecharge" class="refund-limit-card">
          <div>
            <span>{{ locale.t('page.wallet.originalRechargeAmount') }}</span>
            <strong>¥{{ formatMoney(getRechargePaidAmount(selectedRefundRecharge)) }}</strong>
          </div>
          <div>
            <span>{{ locale.t('page.wallet.refundedAmount') }}</span>
            <strong>¥{{ formatMoney(selectedRefundUsedAmount) }}</strong>
          </div>
          <div>
            <span>{{ locale.t('page.wallet.refundableAmount') }}</span>
            <strong class="accent">¥{{ formatMoney(selectedRefundAvailableAmount) }}</strong>
          </div>
        </div>
        <el-form-item :label="locale.t('page.wallet.refundAmount')" prop="refundAmount">
          <el-input-number v-model="refundForm.refundAmount" :min="0.01" :max="refundAmountMax" :precision="2" :disabled="!selectedRefundRecharge" class="wide-control" />
        </el-form-item>
        <el-form-item :label="locale.t('page.wallet.refundReason')" prop="refundReason">
          <el-input v-model="refundForm.refundReason" type="textarea" :rows="3" :placeholder="locale.t('page.wallet.refundReasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="refundSaving" :disabled="!selectedRefundRecharge || selectedRefundAvailableAmount <= 0" @click="handleCreateRefund">{{ locale.t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundAuditDialogVisible" :title="refundAuditMode === 'approve' ? locale.t('page.wallet.approveRefund') : locale.t('page.wallet.rejectRefund')" width="520px" destroy-on-close>
      <el-form ref="refundAuditFormRef" :model="refundAuditForm" label-width="130px">
        <el-form-item v-if="refundAuditMode === 'approve'" :label="locale.t('page.wallet.refundTransactionNo')">
          <el-input v-model.trim="refundAuditForm.refundTransactionNo" :placeholder="locale.t('page.wallet.refundTransactionNoPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.wallet.auditRemark')">
          <el-input v-model="refundAuditForm.auditRemark" type="textarea" :rows="3" :placeholder="locale.t('page.wallet.auditRemarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundAuditDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button :type="refundAuditMode === 'approve' ? 'primary' : 'danger'" :loading="refundAuditing" @click="handleAuditRefund">
          {{ refundAuditMode === 'approve' ? locale.t('page.wallet.approveRefund') : locale.t('page.wallet.rejectRefund') }}
        </el-button>
      </template>
    </el-dialog>
    <el-drawer v-model="refundDetailVisible" :title="locale.t('page.wallet.refundDetail')" size="560px">
      <div v-if="currentRefund" v-loading="refundDetailLoading" class="refund-detail-panel">
        <el-descriptions :title="locale.t('page.wallet.refundInfo')" :column="1" border>
          <el-descriptions-item :label="locale.t('page.wallet.refundNo')">{{ currentRefund.refundNo }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.owner')">{{ getWalletOwnerName(currentRefund.ownerType, currentRefund.ownerId) }} / {{ getWalletOwnerCode(currentRefund.ownerType, currentRefund.ownerId) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.payMethod')">{{ locale.t(`status.payMethod.${currentRefund.payMethod}`, currentRefund.payMethod) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.refundAmount')">¥{{ formatMoney(currentRefund.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.bills.status')">
            <el-tag :type="getRefundStatusType(currentRefund.status)" effect="dark">
              {{ locale.t(`status.refund.${currentRefund.status}`, currentRefund.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.refundReason')">{{ currentRefund.refundReason || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.auditTime')">{{ currentRefund.auditTime || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.auditRemark')">{{ currentRefund.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.transactionNo')">{{ currentRefund.transactionNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.refundTransactionNo')">{{ currentRefund.refundTransactionNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.refundTime')">{{ currentRefund.refundTime || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.failReason')">{{ currentRefund.failReason || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.reservedRefundAmount')">¥{{ formatMoney(currentRefund.reservedAmount) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.gatewayRefundStatus')">{{ currentRefund.gatewayRefundStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.reconcileStatus')">
            {{ locale.t(`status.reconcile.${currentRefund.reconcileStatus || 'pending'}`, currentRefund.reconcileStatus || 'pending') }}
          </el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.reconcileMessage')">{{ currentRefund.reconcileMessage || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.lastSyncTime')">{{ currentRefund.lastSyncTime || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.syncRetryCount')">{{ currentRefund.syncRetryCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="canAuditRefund && currentRefund.reconcileStatus === 'exception'" class="refund-exception-panel">
          <el-alert
            :title="locale.t('page.wallet.reconcileExceptionTitle')"
            :description="currentRefund.reconcileMessage || locale.t('page.wallet.reconcileExceptionHint')"
            type="error"
            show-icon
            :closable="false"
          />
          <div class="refund-exception-actions">
            <el-button type="warning" :loading="refundSyncingId === currentRefund.id" @click="handleSyncRefund(currentRefund)">
              {{ locale.t('page.wallet.retryRefundSync') }}
            </el-button>
            <el-button type="success" @click="openRefundExceptionAction('confirm_refunded')">
              {{ locale.t('page.wallet.confirmGatewayRefunded') }}
            </el-button>
            <el-button v-if="currentRefund.status !== 'refunded'" type="danger" @click="openRefundExceptionAction('release_reservation')">
              {{ locale.t('page.wallet.confirmGatewayFailed') }}
            </el-button>
          </div>
        </div>

        <el-descriptions class="refund-detail-section" :title="locale.t('page.wallet.originalRecharge')" :column="1" border>
          <el-descriptions-item :label="locale.t('page.wallet.rechargeNo')">{{ currentRefund.rechargeNo }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.amount')">¥{{ formatMoney(getRechargePaidAmount(refundDetailRecharge)) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.refundedAmount')">¥{{ formatMoney(refundDetailUsedAmount) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.refundableAmount')">¥{{ formatMoney(refundDetailAvailableAmount) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.bills.status')">{{ refundDetailRecharge ? locale.t(`status.recharge.${refundDetailRecharge.status}`, refundDetailRecharge.status) : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.paidAmount')">¥{{ formatMoney(refundDetailRecharge?.paidAmount || refundDetailRecharge?.amount) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.transactionNo')">{{ refundDetailRecharge?.transactionNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.processedTime')">{{ refundDetailRecharge?.paidTime || refundDetailRecharge?.auditTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="refund-detail-section">
          <h4>{{ locale.t('page.wallet.relatedRefunds') }}</h4>
          <el-table :data="refundDetailRelatedRefunds" class="data-table compact-table" row-key="id">
            <el-table-column prop="refundNo" :label="locale.t('page.wallet.refundNo')" min-width="170" />
            <el-table-column :label="locale.t('page.wallet.refundAmount')" width="120">
              <template #default="{ row }">¥{{ formatMoney(row.refundAmount) }}</template>
            </el-table-column>
            <el-table-column :label="locale.t('page.bills.status')" width="110">
              <template #default="{ row }">{{ locale.t(`status.refund.${row.status}`, row.status) }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div v-if="canAuditRefund" class="refund-detail-section">
          <h4>{{ locale.t('page.wallet.exceptionActionHistory') }}</h4>
          <el-empty v-if="refundReconcileActions.length === 0" :description="locale.t('page.wallet.exceptionActionEmpty')" />
          <div v-else class="reconcile-action-list">
            <article v-for="action in refundReconcileActions" :key="action.id" class="reconcile-action-card">
              <header>
                <strong>{{ locale.t(`status.refundAction.${action.actionType}`, action.actionType) }}</strong>
                <span>{{ action.createTime || '-' }}</span>
              </header>
              <p>{{ action.reason }}</p>
              <div class="reconcile-action-meta">
                <span>{{ locale.t('page.wallet.operatorUser') }}：{{ action.operatorName || '-' }}（{{ action.operatorUserId }}）</span>
                <span>{{ locale.t('page.wallet.refundStatusChange') }}：{{ action.refundStatusBefore || '-' }} → {{ action.refundStatusAfter || '-' }}</span>
                <span>{{ locale.t('page.wallet.availableBalanceChange') }}：¥{{ formatMoney(action.availableBalanceBefore) }} → ¥{{ formatMoney(action.availableBalanceAfter) }}</span>
                <span>{{ locale.t('page.wallet.frozenBalanceChange') }}：¥{{ formatMoney(action.frozenBalanceBefore) }} → ¥{{ formatMoney(action.frozenBalanceAfter) }}</span>
              </div>
              <small>{{ action.resultMessage || '-' }}</small>
            </article>
          </div>
        </div>
      </div>
    </el-drawer>
    <el-dialog
      v-model="refundExceptionDialogVisible"
      :title="locale.t(refundExceptionMode === 'confirm_refunded' ? 'page.wallet.confirmGatewayRefunded' : 'page.wallet.confirmGatewayFailed')"
      width="560px"
      destroy-on-close
    >
      <el-alert
        :title="locale.t(refundExceptionMode === 'confirm_refunded' ? 'page.wallet.confirmRefundedRisk' : 'page.wallet.releaseReservationRisk')"
        type="warning"
        show-icon
        :closable="false"
        class="scope-alert"
      />
      <el-form label-width="130px">
        <el-form-item v-if="refundExceptionMode === 'confirm_refunded'" :label="locale.t('page.wallet.refundTransactionNo')" required>
          <el-input v-model.trim="refundExceptionForm.refundTransactionNo" :placeholder="locale.t('page.wallet.refundTransactionNoPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.wallet.exceptionReason')" required>
          <el-input
            v-model="refundExceptionForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="locale.t('page.wallet.exceptionReasonPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundExceptionDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button
          :type="refundExceptionMode === 'confirm_refunded' ? 'success' : 'danger'"
          :loading="refundExceptionSaving"
          @click="submitRefundExceptionAction"
        >
          {{ locale.t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="scanPayDialogVisible" :title="locale.t('page.wallet.scanPayTitle')" width="460px">
      <div v-if="scanPayOrder" class="scan-pay-panel">
        <div class="scan-pay-summary">
          <strong>{{ locale.t(`status.payMethod.${scanPayOrder.payMethod}`, scanPayOrder.payMethod) }}</strong>
          <span>{{ scanPayOrder.rechargeNo }}</span>
          <b>¥{{ formatMoney(scanPayOrder.amount) }}</b>
        </div>
        <img v-if="scanPayOrder.qrCodeUrl" class="scan-pay-qr" :src="getQrImageUrl(scanPayOrder.qrCodeUrl)" :alt="locale.t('page.wallet.scanPayTitle')" />
        <el-input v-if="scanPayOrder.qrCodeUrl" :model-value="scanPayOrder.qrCodeUrl" readonly>
          <template #append>
            <el-button @click="copyPayUrl(scanPayOrder.qrCodeUrl || '')">{{ locale.t('common.copy') }}</el-button>
          </template>
        </el-input>
        <el-alert class="scope-alert" :title="locale.t('page.wallet.scanPayHint')" type="info" show-icon :closable="false" />
      </div>
      <template #footer>
        <el-button @click="scanPayDialogVisible = false">{{ locale.t('common.close') }}</el-button>
        <el-button type="primary" @click="refreshScanPayStatus(true)">{{ locale.t('page.wallet.refreshRechargeStatus') }}</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { fetchAds, type AdOrder } from '@/api/ads'
import { uploadFile } from '@/api/files'
import { fetchAdvertisers, fetchAgents, type Advertiser, type Agent } from '@/api/partners'
import { fetchPlans, type AdPlan } from '@/api/plans'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import {
  billStatusMap,
  billTypeMap,
  confirmBill,
  fetchBillDetails,
  fetchBills,
  generateBills,
  payBill,
  type AdBill,
  type AdBillDetail
} from '@/api/bills'
import {
  approveRefundOrder,
  confirmRechargeOrder,
  createRechargeOrder,
  createRefundOrder,
  createScanRechargePayment,
  fetchPayCallbackLogs,
  fetchPlayCharges,
  fetchRechargeOrders,
  fetchRefundOrders,
  fetchRefundReconcileActions,
  fetchWalletAccounts,
  handleRefundReconcileAction,
  rejectRefundOrder,
  syncRefundOrder,
  type PayCallbackLog,
  type PlayCharge,
  type RechargePayMethod,
  type RefundOrder,
  type RefundReconcileAction,
  type RechargeOrder,
  type WalletAccount,
  type WalletOwnerType
} from '@/api/wallet'

const loading = ref(false)
const locale = useLocaleStore()
const user = useUserStore()
const activeTab = ref('bills')
const generating = ref(false)
const advertiserLoading = ref(false)
const agentLoading = ref(false)
const drawerVisible = ref(false)
const payDialogVisible = ref(false)
const rechargeDialogVisible = ref(false)
const scanPayDialogVisible = ref(false)
const refundDialogVisible = ref(false)
const refundAuditDialogVisible = ref(false)
const refundDetailVisible = ref(false)
const refundExceptionDialogVisible = ref(false)
const records = ref<AdBill[]>([])
const details = ref<AdBillDetail[]>([])
const walletAccounts = ref<WalletAccount[]>([])
const recharges = ref<RechargeOrder[]>([])
const playCharges = ref<PlayCharge[]>([])
const payCallbackLogs = ref<PayCallbackLog[]>([])
const refunds = ref<RefundOrder[]>([])
const refundableRecharges = ref<RechargeOrder[]>([])
const refundableRefunds = ref<RefundOrder[]>([])
const refundDetailRelatedRefunds = ref<RefundOrder[]>([])
const refundReconcileActions = ref<RefundReconcileAction[]>([])
const advertiserOptions = ref<Advertiser[]>([])
const agentOptions = ref<Agent[]>([])
const adOptions = ref<AdOrder[]>([])
const planOptions = ref<AdPlan[]>([])
const current = ref<AdBill>()
const scanPayOrder = ref<RechargeOrder>()
const currentRefund = ref<RefundOrder>()
const refundDetailRecharge = ref<RechargeOrder>()
const scanPayTimer = ref<number>()
const total = ref(0)
const generateMonth = ref(new Date().toISOString().slice(0, 7))
const paying = ref(false)
const walletLoading = ref(false)
const rechargeLoading = ref(false)
const chargeLoading = ref(false)
const callbackLoading = ref(false)
const refundLoading = ref(false)
const refundSyncingId = ref<number>()
const rechargeSaving = ref(false)
const refundSaving = ref(false)
const refundAuditing = ref(false)
const refundDetailLoading = ref(false)
const refundExceptionSaving = ref(false)
const refundExceptionMode = ref<'confirm_refunded' | 'release_reservation'>('confirm_refunded')
const refundAuditMode = ref<'approve' | 'reject'>('approve')
const voucherUploading = ref(false)
const rechargeVoucherUploading = ref(false)
const payBillId = ref<number>()
const payFormRef = ref<FormInstance>()
const rechargeFormRef = ref<FormInstance>()
const refundFormRef = ref<FormInstance>()
const refundAuditFormRef = ref<FormInstance>()
const payForm = reactive({
  paymentVoucherNo: '',
  paymentVoucherUrl: ''
})
const refundForm = reactive({
  rechargeOrderId: undefined as number | undefined,
  refundAmount: 0,
  refundReason: ''
})
const refundAuditForm = reactive({
  auditRemark: '',
  refundTransactionNo: ''
})
const refundExceptionForm = reactive({
  reason: '',
  refundTransactionNo: ''
})

const rechargeForm = reactive({
  ownerType: 'advertiser' as WalletOwnerType,
  ownerId: undefined as number | undefined,
  payMethod: 'bank_transfer' as RechargePayMethod,
  amount: 1000,
  voucherNo: '',
  voucherUrl: '',
  remark: ''
})

const query = reactive({
  billMonth: '',
  advertiserId: undefined as number | undefined,
  agentId: undefined as number | undefined,
  status: '',
  page: 1,
  size: 10
})
const walletQuery = reactive({ ownerType: undefined as WalletOwnerType | undefined, ownerId: undefined as number | undefined, page: 1, size: 200 })
const rechargeQuery = reactive({ ownerType: undefined as WalletOwnerType | undefined, ownerId: undefined as number | undefined, status: 'auditing', page: 1, size: 200 })
const chargeQuery = reactive({ ownerType: undefined as WalletOwnerType | undefined, ownerId: undefined as number | undefined, status: '', page: 1, size: 200 })
const callbackQuery = reactive({ payMethod: '', rechargeNo: '', transactionNo: '', callbackStatus: '', page: 1, size: 200 })
const refundQuery = reactive({ ownerType: undefined as WalletOwnerType | undefined, ownerId: undefined as number | undefined, payMethod: '', status: 'auditing', reconcileStatus: '', refundNo: '', rechargeNo: '', page: 1, size: 200 })

const stats = computed(() => [
  { label: getStatusLabel('bill', 'pending', '待确认'), value: records.value.filter((item) => item.status === 'pending').length },
  { label: getStatusLabel('bill', 'confirmed', '待支付'), value: records.value.filter((item) => item.status === 'confirmed').length },
  { label: getStatusLabel('bill', 'paid', '已支付'), value: records.value.filter((item) => item.status === 'paid').length },
  { label: locale.t('page.bills.currentAmount'), value: `¥${formatMoney(records.value.reduce((sum, item) => sum + Number(item.amountTotal || 0), 0))}` }
])

const payRules = computed<FormRules>(() => ({
  paymentVoucherNo: [{ required: true, message: locale.t('page.bills.paymentVoucherNoRequired'), trigger: 'blur' }],
  paymentVoucherUrl: [{ required: true, message: locale.t('page.bills.paymentVoucherUrlRequired'), trigger: 'change' }]
}))

const canViewWalletAccounts = computed(() => user.hasPermission('wallet:view'))
const canCreateRecharge = computed(() => user.hasPermission('wallet:recharge:create'))
const canAuditRecharge = computed(() => user.hasPermission('wallet:recharge:audit'))
const canViewRecharges = computed(() => user.hasPermission('wallet:recharge:view') || canCreateRecharge.value || canAuditRecharge.value)
const canViewCharges = computed(() => user.hasPermission('wallet:charge:view'))
const canViewPayCallbacks = computed(() => canAuditRecharge.value)
const canCreateRefund = computed(() => user.hasPermission('wallet:refund:create'))
const canAuditRefund = computed(() => user.hasPermission('wallet:refund:audit'))
const canViewRefunds = computed(() => user.hasPermission('wallet:refund:view') || canCreateRefund.value || canAuditRefund.value)
const isBankRecharge = computed(() => rechargeForm.payMethod === 'bank_transfer')

function validateRefundAmount(_rule: unknown, value: unknown, callback: (error?: Error) => void) {
  const amount = Number(value || 0)
  if (amount <= 0) {
    callback(new Error(locale.t('page.wallet.refundAmountRequired')))
    return
  }
  if (selectedRefundRecharge.value && amount > selectedRefundAvailableAmount.value) {
    callback(new Error(locale.t('page.wallet.refundAmountExceeded')))
    return
  }
  callback()
}

const refundRules = computed<FormRules>(() => ({
  rechargeOrderId: [{ required: true, message: locale.t('page.wallet.rechargeOrderRequired'), trigger: 'change' }],
  refundAmount: [{ required: true, validator: validateRefundAmount, trigger: ['blur', 'change'] }]
}))

const rechargeRules = computed<FormRules>(() => {
  const rules: FormRules = {
    ownerType: [{ required: true, message: locale.t('page.wallet.ownerTypeRequired'), trigger: 'change' }],
    ownerId: [{ required: true, message: locale.t('page.wallet.ownerRequired'), trigger: 'change' }],
    payMethod: [{ required: true, message: locale.t('page.wallet.payMethodRequired'), trigger: 'change' }],
    amount: [{ required: true, message: locale.t('page.wallet.amountRequired'), trigger: 'blur' }]
  }
  if (isBankRecharge.value) {
    rules.voucherNo = [{ required: true, message: locale.t('page.bills.paymentVoucherNoRequired'), trigger: 'blur' }]
    rules.voucherUrl = [{ required: true, message: locale.t('page.bills.paymentVoucherUrlRequired'), trigger: 'change' }]
  }
  return rules
})

const rechargeOwnerOptions = computed(() => {
  if (rechargeForm.ownerType === 'agent') {
    return agentOptions.value.map((item) => ({ id: item.id, label: getAgentLabel(item) }))
  }
  return advertiserOptions.value.map((item) => ({ id: item.id, label: getAdvertiserLabel(item) }))
})

const currentBusinessOwnerText = computed(() => {
  const info = user.userInfo
  if (info?.userType === 'agent') {
    return getWalletOwnerName('agent', info.agentId) + ' / ' + getWalletOwnerCode('agent', info.agentId)
  }
  if (info?.userType === 'advertiser') {
    return getWalletOwnerName('advertiser', info.advertiserId) + ' / ' + getWalletOwnerCode('advertiser', info.advertiserId)
  }
  return '-'
})

const activeRefundStatuses = ['auditing', 'processing', 'refunded', 'approved']

const selectedRefundRecharge = computed(() => refundableRecharges.value.find((item) => item.id === refundForm.rechargeOrderId))
const selectedRefundUsedAmount = computed(() => getRechargeRefundedAmount(selectedRefundRecharge.value, refundableRefunds.value))
const selectedRefundAvailableAmount = computed(() => getRechargeRefundableAmount(selectedRefundRecharge.value, refundableRefunds.value))
const refundAmountMax = computed(() => (selectedRefundRecharge.value ? Math.max(0.01, selectedRefundAvailableAmount.value) : undefined))
const refundDetailUsedAmount = computed(() => getRechargeRefundedAmount(refundDetailRecharge.value, refundDetailRelatedRefunds.value))
const refundDetailAvailableAmount = computed(() => getRechargeRefundableAmount(refundDetailRecharge.value, refundDetailRelatedRefunds.value))

function getRechargePaidAmount(row?: RechargeOrder) {
  return Number(row?.paidAmount ?? row?.amount ?? 0)
}

function getRechargeRefundedAmount(row?: RechargeOrder, refundRecords: RefundOrder[] = []) {
  if (!row) {
    return 0
  }
  return refundRecords
    .filter((item) => item.rechargeNo === row.rechargeNo && activeRefundStatuses.includes(item.status))
    .reduce((sum, item) => sum + Number(item.refundAmount || 0), 0)
}

function getRechargeRefundableAmount(row?: RechargeOrder, refundRecords: RefundOrder[] = []) {
  const available = getRechargePaidAmount(row) - getRechargeRefundedAmount(row, refundRecords)
  return Math.max(0, Number(available.toFixed(2)))
}


function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getStringStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

function getAdvertiserLabel(advertiser: Advertiser) {
  return `${advertiser.advertiserName}（${advertiser.advertiserCode}）`
}

function getAgentLabel(agent: Agent) {
  return `${agent.agentName}（${agent.agentCode}）`
}

function findAdvertiser(advertiserId?: number) {
  return advertiserOptions.value.find((item) => item.id === advertiserId)
}

function findAgent(agentId?: number) {
  return agentOptions.value.find((item) => item.id === agentId)
}

function findAd(adId?: number) {
  return adOptions.value.find((item) => item.id === adId)
}

function findPlan(planId?: number) {
  return planOptions.value.find((item) => item.id === planId)
}

function getBillSubjectName(row?: AdBill) {
  if (!row) {
    return '-'
  }
  if (row.billType === 'agent') {
    return findAgent(row.agentId)?.agentName || (row.agentId ? `#${row.agentId}` : '-')
  }
  return findAdvertiser(row.advertiserId)?.advertiserName || (row.advertiserId ? `#${row.advertiserId}` : '-')
}

function getBillSubjectCode(row?: AdBill) {
  if (!row) {
    return '-'
  }
  if (row.billType === 'agent') {
    return findAgent(row.agentId)?.agentCode || (row.agentId ? `ID ${row.agentId}` : '-')
  }
  return findAdvertiser(row.advertiserId)?.advertiserCode || (row.advertiserId ? `ID ${row.advertiserId}` : '-')
}

function getWalletOwnerName(ownerType?: WalletOwnerType, ownerId?: number) {
  if (ownerType === 'agent') {
    return findAgent(ownerId)?.agentName || (ownerId ? `#${ownerId}` : '-')
  }
  return findAdvertiser(ownerId)?.advertiserName || (ownerId ? `#${ownerId}` : '-')
}

function getWalletOwnerCode(ownerType?: WalletOwnerType, ownerId?: number) {
  if (ownerType === 'agent') {
    return findAgent(ownerId)?.agentCode || (ownerId ? `ID ${ownerId}` : '-')
  }
  return findAdvertiser(ownerId)?.advertiserCode || (ownerId ? `ID ${ownerId}` : '-')
}

function getAdName(adId?: number) {
  return findAd(adId)?.adName || (adId ? `#${adId}` : '-')
}

function getAdCode(adId?: number) {
  return findAd(adId)?.adCode || (adId ? `ID ${adId}` : '-')
}

function getPlanName(planId?: number) {
  return findPlan(planId)?.planName || (planId ? `#${planId}` : '-')
}

function getPlanCode(planId?: number) {
  return findPlan(planId)?.planCode || (planId ? `ID ${planId}` : '-')
}

function getRechargeStatusType(value?: string) {
  if (value === 'paid' || value === 'confirmed') {
    return 'success'
  }
  if (value === 'rejected') {
    return 'danger'
  }
  return 'warning'
}

function getChargeStatusType(value?: string) {
  if (value === 'success') {
    return 'success'
  }
  if (value === 'failed') {
    return 'danger'
  }
  return 'info'
}


function getRefundStatusType(value?: string) {
  if (value === 'refunded' || value === 'approved') {
    return 'success'
  }
  if (value === 'rejected' || value === 'failed') {
    return 'danger'
  }
  if (value === 'processing') {
    return 'primary'
  }
  return 'warning'
}

function getReconcileStatusType(value?: string) {
  if (value === 'matched') {
    return 'success'
  }
  if (value === 'exception') {
    return 'danger'
  }
  return 'warning'
}
function getCallbackStatusType(value?: string) {
  if (value === 'success') {
    return 'success'
  }
  if (value === 'duplicate') {
    return 'warning'
  }
  if (value === 'failed') {
    return 'danger'
  }
  return 'info'
}
function handleTabChange(name: string | number) {
  if (name === 'accounts') {
    loadWalletAccounts()
  }
  if (name === 'recharges') {
    loadRecharges()
  }
  if (name === 'charges') {
    loadPlayCharges()
  }
  if (name === 'refunds') {
    loadRefunds()
  }
}

function applyBusinessWalletScope() {
  const info = user.userInfo
  if (user.isPlatformScope || !info) {
    return
  }
  if (info.userType === 'advertiser') {
    walletQuery.ownerType = 'advertiser'
    walletQuery.ownerId = info.advertiserId
    rechargeQuery.ownerType = 'advertiser'
    rechargeQuery.ownerId = info.advertiserId
    chargeQuery.ownerType = 'advertiser'
    chargeQuery.ownerId = info.advertiserId
    refundQuery.ownerType = 'advertiser'
    refundQuery.ownerId = info.advertiserId
  }
  if (info.userType === 'agent') {
    walletQuery.ownerType = 'agent'
    walletQuery.ownerId = info.agentId
    rechargeQuery.ownerType = 'agent'
    rechargeQuery.ownerId = info.agentId
    chargeQuery.ownerType = 'agent'
    chargeQuery.ownerId = info.agentId
    refundQuery.ownerType = 'agent'
    refundQuery.ownerId = info.agentId
  }
}

async function loadReferenceData() {
  advertiserLoading.value = true
  agentLoading.value = true
  try {
    const [advertisers, agents, ads, plans] = await Promise.all([
      fetchAdvertisers({ page: 1, size: 500 }),
      fetchAgents({ page: 1, size: 500 }),
      fetchAds({ page: 1, size: 500 }),
      fetchPlans({ page: 1, size: 500 })
    ])
    advertiserOptions.value = advertisers.data.records
    agentOptions.value = agents.data.records
    adOptions.value = ads.data.records
    planOptions.value = plans.data.records
  } finally {
    advertiserLoading.value = false
    agentLoading.value = false
  }
}

async function loadBills() {
  loading.value = true
  try {
    const result = await fetchBills(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

async function loadWalletAccounts() {
  walletLoading.value = true
  try {
    const result = await fetchWalletAccounts(walletQuery)
    walletAccounts.value = result.data.records
  } finally {
    walletLoading.value = false
  }
}

async function loadRecharges() {
  rechargeLoading.value = true
  try {
    const result = await fetchRechargeOrders(rechargeQuery)
    recharges.value = result.data.records
  } finally {
    rechargeLoading.value = false
  }
}


async function loadRefunds() {
  refundLoading.value = true
  try {
    const result = await fetchRefundOrders(refundQuery)
    refunds.value = result.data.records
  } finally {
    refundLoading.value = false
  }
}

async function loadPayCallbackLogs() {
  callbackLoading.value = true
  try {
    const result = await fetchPayCallbackLogs(callbackQuery)
    payCallbackLogs.value = result.data.records
  } finally {
    callbackLoading.value = false
  }
}
async function loadPlayCharges() {
  chargeLoading.value = true
  try {
    const result = await fetchPlayCharges(chargeQuery)
    playCharges.value = result.data.records
  } finally {
    chargeLoading.value = false
  }
}

function resetQuery() {
  query.billMonth = ''
  query.advertiserId = undefined
  query.agentId = undefined
  query.status = ''
  query.page = 1
  loadBills()
}

function resetWalletQuery() {
  Object.assign(walletQuery, { ownerType: undefined, ownerId: undefined, page: 1 })
  applyBusinessWalletScope()
  loadWalletAccounts()
}

function resetRechargeQuery() {
  Object.assign(rechargeQuery, { ownerType: undefined, ownerId: undefined, status: 'auditing', page: 1 })
  applyBusinessWalletScope()
  loadRecharges()
}


function resetRefundQuery() {
  Object.assign(refundQuery, { ownerType: undefined, ownerId: undefined, payMethod: '', status: 'auditing', reconcileStatus: '', refundNo: '', rechargeNo: '', page: 1 })
  applyBusinessWalletScope()
  loadRefunds()
}

function resetCallbackQuery() {
  Object.assign(callbackQuery, { payMethod: '', rechargeNo: '', transactionNo: '', callbackStatus: '', page: 1 })
  loadPayCallbackLogs()
}
function resetChargeQuery() {
  Object.assign(chargeQuery, { ownerType: undefined, ownerId: undefined, status: '', page: 1 })
  applyBusinessWalletScope()
  loadPlayCharges()
}

async function handleGenerate() {
  if (!generateMonth.value) {
    ElMessage.warning(locale.t('page.bills.monthRequired'))
    return
  }
  generating.value = true
  try {
    const result = await generateBills(generateMonth.value)
    ElMessage.success(locale.t('page.bills.generated').replace('{count}', String(result.data.length)))
    query.billMonth = generateMonth.value
    loadBills()
  } finally {
    generating.value = false
  }
}

async function openDetails(row: AdBill) {
  current.value = row
  const result = await fetchBillDetails(row.id)
  details.value = result.data
  drawerVisible.value = true
}

async function handleConfirm(id: number) {
  await confirmBill(id)
  ElMessage.success(locale.t('page.bills.confirmed'))
  loadBills()
}

function openPayDialog(row: AdBill) {
  payBillId.value = row.id
  payForm.paymentVoucherNo = row.paymentVoucherNo || ''
  payForm.paymentVoucherUrl = row.paymentVoucherUrl || ''
  payDialogVisible.value = true
}

function beforeVoucherUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning(locale.t('page.bills.voucherImageOnly'))
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning(locale.t('page.bills.voucherTooLarge'))
    return false
  }
  return true
}

async function handleVoucherUpload(options: UploadRequestOptions) {
  voucherUploading.value = true
  try {
    const result = await uploadFile(options.file as File, 'bill-vouchers')
    payForm.paymentVoucherUrl = result.data.url
    options.onSuccess?.(result)
    ElMessage.success(locale.t('page.bills.voucherUploaded'))
  } catch (error) {
    options.onError?.(error as never)
  } finally {
    voucherUploading.value = false
  }
}

async function handleRechargeVoucherUpload(options: UploadRequestOptions) {
  rechargeVoucherUploading.value = true
  try {
    const result = await uploadFile(options.file as File, 'wallet-vouchers')
    rechargeForm.voucherUrl = result.data.url
    options.onSuccess?.(result)
    ElMessage.success(locale.t('page.bills.voucherUploaded'))
  } catch (error) {
    options.onError?.(error as never)
  } finally {
    rechargeVoucherUploading.value = false
  }
}

function syncBusinessRechargeOwner() {
  const info = user.userInfo
  if (user.isPlatformScope || !info) {
    return
  }
  if (info.userType === 'agent') {
    rechargeForm.ownerType = 'agent'
    rechargeForm.ownerId = info.agentId
  }
  if (info.userType === 'advertiser') {
    rechargeForm.ownerType = 'advertiser'
    rechargeForm.ownerId = info.advertiserId
  }
}

function openRechargeDialog() {
  const info = user.userInfo
  Object.assign(rechargeForm, {
    ownerType: info?.userType === 'agent' ? 'agent' : 'advertiser',
    ownerId: info?.userType === 'agent' ? info.agentId : (info?.userType === 'advertiser' ? info.advertiserId : undefined),
    payMethod: 'bank_transfer',
    amount: 1000,
    voucherNo: '',
    voucherUrl: '',
    remark: ''
  })
  syncBusinessRechargeOwner()
  rechargeDialogVisible.value = true
}

async function handleCreateRecharge() {
  syncBusinessRechargeOwner()
  await rechargeFormRef.value?.validate()
  if (!rechargeForm.ownerId) {
    ElMessage.warning(locale.t('page.wallet.ownerRequired'))
    return
  }
  rechargeSaving.value = true
  try {
    const payload = {
      ownerType: rechargeForm.ownerType,
      ownerId: rechargeForm.ownerId,
      payMethod: rechargeForm.payMethod,
      amount: rechargeForm.amount,
      voucherNo: rechargeForm.voucherNo,
      voucherUrl: rechargeForm.voucherUrl,
      remark: rechargeForm.remark
    }
    if (isBankRecharge.value) {
      await createRechargeOrder(payload)
      ElMessage.success(locale.t('page.wallet.rechargeCreated'))
    } else {
      const result = await createScanRechargePayment(payload)
      scanPayOrder.value = result.data.order
      scanPayDialogVisible.value = true
      startScanPayPolling()
      rechargeQuery.status = ''
      ElMessage.success(locale.t('page.wallet.scanRechargeCreated'))
    }
    rechargeDialogVisible.value = false
    activeTab.value = 'recharges'
    await loadRecharges()
  } finally {
    rechargeSaving.value = false
  }
}

function getRechargeOptionLabel(row: RechargeOrder) {
  return `${row.rechargeNo} / ${getWalletOwnerName(row.ownerType, row.ownerId)} / ${locale.t('page.wallet.refundableAmount')} ¥${formatMoney(getRechargeRefundableAmount(row, refundableRefunds.value))}`
}

async function openRefundDialog() {
  Object.assign(refundForm, { rechargeOrderId: undefined, refundAmount: 0, refundReason: '' })
  await loadRefundableRecharges()
  refundDialogVisible.value = true
}

async function loadRefundableRecharges() {
  syncBusinessRechargeOwner()
  const ownerType = user.isPlatformScope ? undefined : rechargeForm.ownerType
  const ownerId = user.isPlatformScope ? undefined : rechargeForm.ownerId
  const [rechargeResult, refundResult] = await Promise.all([
    fetchRechargeOrders({
      ownerType,
      ownerId,
      status: 'paid',
      page: 1,
      size: 500
    }),
    fetchRefundOrders({
      ownerType,
      ownerId,
      status: '',
      page: 1,
      size: 1000
    })
  ])
  refundableRefunds.value = refundResult.data.records
  refundableRecharges.value = rechargeResult.data.records.filter((item) => getRechargeRefundableAmount(item, refundableRefunds.value) > 0)
}

function onRefundRechargeChange(value?: number) {
  const selected = refundableRecharges.value.find((item) => item.id === value)
  refundForm.refundAmount = selected ? getRechargeRefundableAmount(selected, refundableRefunds.value) : 0
}

async function handleCreateRefund() {
  if (!refundForm.rechargeOrderId || !selectedRefundRecharge.value) {
    ElMessage.warning(locale.t('page.wallet.rechargeOrderRequired'))
    return
  }
  await refundFormRef.value?.validate()
  refundSaving.value = true
  try {
    const selected = selectedRefundRecharge.value
    const latestRefundResult = await fetchRefundOrders({
      ownerType: selected.ownerType,
      ownerId: selected.ownerId,
      status: '',
      rechargeNo: selected.rechargeNo,
      page: 1,
      size: 1000
    })
    refundableRefunds.value = [
      ...refundableRefunds.value.filter((item) => item.rechargeNo !== selected.rechargeNo),
      ...latestRefundResult.data.records
    ]
    const latestAvailableAmount = getRechargeRefundableAmount(selected, refundableRefunds.value)
    if (latestAvailableAmount <= 0) {
      ElMessage.warning(locale.t('page.wallet.noRefundableAmount'))
      return
    }
    if (refundForm.refundAmount > latestAvailableAmount) {
      ElMessage.warning(locale.t('page.wallet.refundAmountExceeded'))
      await refundFormRef.value?.validateField('refundAmount').catch(() => undefined)
      return
    }
    await createRefundOrder({
      rechargeOrderId: refundForm.rechargeOrderId,
      refundAmount: refundForm.refundAmount,
      refundReason: refundForm.refundReason
    })
    ElMessage.success(locale.t('page.wallet.refundCreated'))
    refundDialogVisible.value = false
    activeTab.value = 'refunds'
    await loadRefunds()
  } finally {
    refundSaving.value = false
  }
}

async function openRefundDetail(row: RefundOrder) {
  currentRefund.value = row
  refundDetailRecharge.value = undefined
  refundDetailRelatedRefunds.value = []
  refundReconcileActions.value = []
  refundDetailVisible.value = true
  refundDetailLoading.value = true
  try {
    const [rechargeResult, refundResult] = await Promise.all([
      fetchRechargeOrders({
        ownerType: row.ownerType,
        ownerId: row.ownerId,
        status: '',
        rechargeNo: row.rechargeNo,
        page: 1,
        size: 1
      }),
      fetchRefundOrders({
        ownerType: row.ownerType,
        ownerId: row.ownerId,
        status: '',
        rechargeNo: row.rechargeNo,
        page: 1,
        size: 500
      })
    ])
    refundDetailRecharge.value = rechargeResult.data.records.find((item) => item.id === row.rechargeOrderId || item.rechargeNo === row.rechargeNo)
    refundDetailRelatedRefunds.value = refundResult.data.records
    if (canAuditRefund.value) {
      await loadRefundReconcileActions(row.id)
    }
  } finally {
    refundDetailLoading.value = false
  }
}

async function loadRefundReconcileActions(refundId: number) {
  const result = await fetchRefundReconcileActions(refundId)
  refundReconcileActions.value = result.data
}

async function handleSyncRefund(row: RefundOrder) {
  refundSyncingId.value = row.id
  try {
    const result = await syncRefundOrder(row.id)
    ElMessage.success(result.data.reconcileStatus === 'matched'
      ? locale.t('page.wallet.refundReconciled')
      : locale.t('page.wallet.refundSyncCompleted'))
    await Promise.all([loadRefunds(), loadWalletAccounts()])
    if (refundDetailVisible.value && currentRefund.value?.id === row.id) {
      await openRefundDetail(result.data)
    }
  } finally {
    refundSyncingId.value = undefined
  }
}

function openRefundExceptionAction(mode: 'confirm_refunded' | 'release_reservation') {
  if (!currentRefund.value) {
    return
  }
  refundExceptionMode.value = mode
  refundExceptionForm.reason = ''
  refundExceptionForm.refundTransactionNo = currentRefund.value.refundTransactionNo || ''
  refundExceptionDialogVisible.value = true
}

async function submitRefundExceptionAction() {
  if (!currentRefund.value) {
    return
  }
  const reason = refundExceptionForm.reason.trim()
  if (!reason) {
    ElMessage.warning(locale.t('page.wallet.exceptionReasonRequired'))
    return
  }
  if (refundExceptionMode.value === 'confirm_refunded' && !refundExceptionForm.refundTransactionNo.trim()) {
    ElMessage.warning(locale.t('page.wallet.refundTransactionNoRequired'))
    return
  }
  try {
    await ElMessageBox.confirm(
      locale.t(refundExceptionMode.value === 'confirm_refunded'
        ? 'page.wallet.confirmRefundedRisk'
        : 'page.wallet.releaseReservationRisk'),
      locale.t('page.wallet.exceptionConfirmTitle'),
      {
        confirmButtonText: locale.t('common.confirm'),
        cancelButtonText: locale.t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }
  refundExceptionSaving.value = true
  try {
    const result = await handleRefundReconcileAction(currentRefund.value.id, {
      actionType: refundExceptionMode.value,
      reason,
      refundTransactionNo: refundExceptionForm.refundTransactionNo.trim() || undefined
    })
    ElMessage.success(locale.t('page.wallet.exceptionHandled'))
    refundExceptionDialogVisible.value = false
    await Promise.all([loadRefunds(), loadWalletAccounts()])
    await openRefundDetail(result.data)
  } finally {
    refundExceptionSaving.value = false
  }
}

function openRefundAudit(row: RefundOrder, mode: 'approve' | 'reject') {
  currentRefund.value = row
  refundAuditMode.value = mode
  refundAuditForm.auditRemark = ''
  refundAuditForm.refundTransactionNo = ''
  refundAuditDialogVisible.value = true
}

async function handleAuditRefund() {
  if (!currentRefund.value) {
    return
  }
  refundAuditing.value = true
  try {
    if (refundAuditMode.value === 'approve') {
      const result = await approveRefundOrder(currentRefund.value.id, refundAuditForm)
      ElMessage.success(result.data.status === 'refunded'
        ? locale.t('page.wallet.refundApproved')
        : locale.t('page.wallet.refundProcessing'))
    } else {
      await rejectRefundOrder(currentRefund.value.id, refundAuditForm)
      ElMessage.success(locale.t('page.wallet.refundRejected'))
    }
    refundAuditDialogVisible.value = false
    await Promise.all([loadRefunds(), loadWalletAccounts()])
  } finally {
    refundAuditing.value = false
  }
}

function openScanPayDialog(row: RechargeOrder) {
  scanPayOrder.value = row
  scanPayDialogVisible.value = true
  startScanPayPolling()
}

function startScanPayPolling() {
  stopScanPayPolling()
  if (!scanPayOrder.value || scanPayOrder.value.status === 'paid' || scanPayOrder.value.status === 'confirmed') {
    return
  }
  scanPayTimer.value = window.setInterval(() => {
    refreshScanPayStatus(false)
  }, 3000)
}

function stopScanPayPolling() {
  if (scanPayTimer.value) {
    window.clearInterval(scanPayTimer.value)
    scanPayTimer.value = undefined
  }
}

async function refreshScanPayStatus(showMessage = false) {
  const currentOrder = scanPayOrder.value
  if (!currentOrder) {
    return
  }
  const result = await fetchRechargeOrders({
    ownerType: currentOrder.ownerType,
    ownerId: currentOrder.ownerId,
    status: '',
    page: 1,
    size: 200
  })
  const latest = result.data.records.find((item) => item.id === currentOrder.id)
  if (!latest) {
    if (showMessage) {
      ElMessage.warning(locale.t('page.wallet.rechargeEmpty'))
    }
    return
  }
  scanPayOrder.value = latest
  if (latest.status === 'paid' || latest.status === 'confirmed') {
    stopScanPayPolling()
    scanPayDialogVisible.value = false
    ElMessage.success(locale.t('page.wallet.scanPayPaid'))
    await Promise.all([loadRecharges(), loadWalletAccounts()])
    return
  }
  if (showMessage) {
    ElMessage.info(locale.t('page.wallet.scanPayWaiting'))
  }
}

function getQrImageUrl(value: string) {
  return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(value)}`
}

async function copyPayUrl(value: string) {
  await navigator.clipboard.writeText(value)
  ElMessage.success(locale.t('common.copied'))
}

async function handleConfirmRecharge(row: RechargeOrder) {
  await confirmRechargeOrder(row.id, {
    transactionNo: row.transactionNo || row.voucherNo,
    remark: locale.t('page.wallet.manualConfirmed')
  })
  ElMessage.success(locale.t('page.wallet.rechargeConfirmed'))
  await Promise.all([loadRecharges(), loadWalletAccounts()])
}

async function handlePay() {
  if (!payBillId.value) {
    return
  }
  await payFormRef.value?.validate()
  paying.value = true
  try {
    await payBill(payBillId.value, {
      paymentVoucherNo: payForm.paymentVoucherNo,
      paymentVoucherUrl: payForm.paymentVoucherUrl
    })
    ElMessage.success(locale.t('page.bills.paid'))
    payDialogVisible.value = false
    loadBills()
  } finally {
    paying.value = false
  }
}

onMounted(() => {
  loadReferenceData()
  applyBusinessWalletScope()
  loadBills()
  if (activeTab.value === 'accounts' && canViewWalletAccounts.value) {
    loadWalletAccounts()
  }
})

onBeforeUnmount(() => {
  stopScanPayPolling()
})

watch(
  () => rechargeForm.ownerType,
  () => {
    if (user.isPlatformScope) {
      rechargeForm.ownerId = undefined
    }
  }
)

watch(
  () => rechargeForm.payMethod,
  () => {
    rechargeFormRef.value?.clearValidate(['voucherNo', 'voucherUrl'])
  }
)

watch(
  () => scanPayDialogVisible.value,
  (visible) => {
    if (!visible) {
      stopScanPayPolling()
    }
  }
)
</script>

<style scoped>
.finance-tabs {
  --el-tabs-header-height: 46px;
}

.detail-table {
  margin-top: 18px;
}

.wide-control {
  width: 100%;
}

.scope-alert {
  margin-bottom: 16px;
}

.refund-limit-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: -2px 0 18px 120px;
}

.refund-limit-card > div {
  padding: 12px 14px;
  border: 1px solid rgba(83, 229, 255, 0.18);
  border-radius: 12px;
  background: rgba(6, 31, 43, 0.82);
}

.refund-limit-card span {
  display: block;
  margin-bottom: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.refund-limit-card strong {
  color: var(--text-primary);
  font-size: 18px;
}

.refund-limit-card .accent {
  color: #53e5ff;
}

.refund-detail-panel {
  display: grid;
  gap: 18px;
}

.refund-detail-section {
  margin-top: 18px;
}

.refund-detail-section h4 {
  margin: 0 0 12px;
  color: var(--text-primary);
}

.compact-table {
  --el-table-row-height: 44px;
}

.refund-exception-panel {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid rgba(255, 95, 95, 0.32);
  border-radius: 12px;
  background: rgba(92, 18, 29, 0.2);
}

.refund-exception-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.reconcile-action-list {
  display: grid;
  gap: 12px;
}

.reconcile-action-card {
  padding: 14px;
  border: 1px solid rgba(83, 229, 255, 0.16);
  border-radius: 10px;
  background: rgba(6, 31, 43, 0.72);
}

.reconcile-action-card header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-primary);
}

.reconcile-action-card header span,
.reconcile-action-card small {
  color: var(--text-secondary);
}

.reconcile-action-card p {
  margin: 10px 0;
  color: var(--text-primary);
}

.reconcile-action-meta {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}
.voucher-upload {
  display: grid;
  width: 100%;
  gap: 10px;
}

.voucher-preview {
  width: 180px;
  height: 110px;
  border: 1px solid rgba(83, 229, 255, 0.18);
  border-radius: 10px;
}

.scan-pay-panel {
  display: grid;
  gap: 18px;
  justify-items: center;
}

.scan-pay-summary {
  display: grid;
  gap: 4px;
  justify-items: center;
  color: var(--text-primary);
}

.scan-pay-summary span {
  color: var(--text-secondary);
}

.scan-pay-summary b {
  color: var(--accent-strong);
  font-size: 26px;
}

.scan-pay-qr {
  width: 240px;
  height: 240px;
  padding: 12px;
  border-radius: 18px;
  background: #fff;
}
</style>
