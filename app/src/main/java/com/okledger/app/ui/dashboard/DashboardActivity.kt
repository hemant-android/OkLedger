package com.okledger.app.ui.dashboard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.okledger.app.R
import com.okledger.app.base.BaseActivity
import com.okledger.app.data.model.PartyWithTotals
import com.okledger.app.databinding.ActivityDashboardBinding
import com.okledger.app.databinding.DrawerHeaderBinding
import com.okledger.app.ui.addparty.AddPartyActivity
import com.okledger.app.ui.editprofile.EditProfileActivity
import com.okledger.app.ui.login.EnterMobileActivity
import com.okledger.app.ui.partytransaction.PartyTransactionActivity
import com.okledger.app.ui.statement.StatementActivity
import com.okledger.app.ui.viewmodel.DashboardViewModel
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class DashboardActivity : BaseActivity<ActivityDashboardBinding>() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: PartyAdapter

    // Header Views
    private lateinit var headerBinding: DrawerHeaderBinding

    private var fullPartyList: List<PartyWithTotals> = emptyList()


    override fun getViewBinding() = ActivityDashboardBinding.inflate(layoutInflater)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupTabs()
        setupDrawerMenu()
        setupRecyclerView()
        setupObservers()
        setSearch()
        setupFabAndEmptyView()
    }

    override fun onResume() {
        super.onResume()
        setupDrawerHeader()
    }

    /** Toolbar setup */
    private fun setupToolbar() {
        binding.toolbar.tvTitle.text = getString(R.string.text_title_dashboard)
        binding.toolbar.imgMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /** Drawer Header binding & click listeners */
    @SuppressLint("SetTextI18n")
    private fun setupDrawerHeader() {
        // Remove previous header to avoid duplicates
        if (binding.navigationView.headerCount > 0) {
            binding.navigationView.removeHeaderView(binding.navigationView.getHeaderView(0))
        }

        // Inflate header using ViewBinding
        headerBinding = DrawerHeaderBinding.inflate(layoutInflater)
        binding.navigationView.addHeaderView(headerBinding.root)

        // Set mobile number
        val mobile = prefs.getMobile()

        if (!mobile.isNullOrBlank()) {
            headerBinding.tvUserContact.visibility = View.VISIBLE
            headerBinding.tvUserContact.text = "+91 $mobile"
        } else {
            headerBinding.tvUserContact.visibility = View.GONE
        }
        if (!prefs.getName().isNullOrBlank()) {
            headerBinding.tvName.visibility = View.VISIBLE
            headerBinding.tvName.text = prefs.getName()
        } else {
            headerBinding.tvName.visibility = View.GONE
        }

        // Profile click listeners
        headerBinding.imgEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            binding.drawerLayout.closeDrawers()
        }

        headerBinding.imgProfile.setOnClickListener {
            showToast("Profile clicked")
        }
    }

    private fun setupTabs() {
        // Add tabs dynamically from enum
        LedgerType.entries.forEach { type ->
            binding.tabLayout.addTab(
                binding.tabLayout.newTab().setText(type.name.capitalize(Locale.ROOT))
            )
        }

        // Select tab based on current ViewModel ledger type
        val currentLedger = viewModel.selectedLedgerType.value ?: LedgerType.PURCHASES
        binding.tabLayout.getTabAt(currentLedger.ordinal)?.select()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedType = LedgerType.entries[tab.position]
                viewModel.setLedgerType(selectedType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }


    /** Navigation Drawer menu click listeners */
    private fun setupDrawerMenu() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> binding.drawerLayout.closeDrawers()
                R.id.nav_parties -> {
                    startActivity(Intent(this, AddPartyActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                }

                R.id.nav_statement -> {
                    startActivity(Intent(this, StatementActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                }

                R.id.nav_logout -> showLogoutDialog()
            }
            true
        }
    }

    private fun showToastAndCloseDrawer(message: String) {
        showToast(message)
        binding.drawerLayout.closeDrawers()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, EnterMobileActivity::class.java))
                finish()
                binding.drawerLayout.closeDrawers()
            }
            .setNegativeButton("No", null)
            .show()
    }

    /** RecyclerView setup */
    private fun setupRecyclerView() {
        adapter = PartyAdapter { party ->
            val intent = Intent(this, PartyTransactionActivity::class.java).apply {
                putExtra("partyId", party.party.id)
                putExtra("partyName", party.party.name)
                putExtra("ledgerType", viewModel.selectedLedgerType.value?.name)
            }
            startActivity(intent)
        }
        binding.rvParties.layoutManager = LinearLayoutManager(this)
        binding.rvParties.adapter = adapter
    }

    private fun setSearch() {
        binding.etSearch.addTextChangedListener { text ->
//            viewModel.setSearchQuery(text.toString())
            applySearchFilter(text.toString())
        }

    }

    /** Observe LiveData from ViewModel */
    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.partyWithTotalsList.observe(this) { parties ->
            if (fullPartyList.isNotEmpty()) {
                fullPartyList = emptyList()
            }
            fullPartyList = parties
            adapter.submitList(parties)
            updateUIForPartyList(parties)
        }

        viewModel.filteredParties.observe(this) { parties ->
            adapter.submitList(parties)
            updateUIForPartyList(parties)
        }


        viewModel.dashboardSummary.observe(this) { summary ->
            updateDashboardSummary(summary.totalGiven, summary.totalReceived)
        }
    }

    /** Update UI based on party list availability */
    private fun updateUIForPartyList(parties: List<Any>) {
        val hasData = parties.isNotEmpty()
        binding.etSearch.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.rvParties.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.layoutSummary.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.emptyView.root.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private fun applySearchFilter(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())

        val filtered = if (q.isEmpty()) {
            fullPartyList
        } else {
            fullPartyList.filter { item ->
                item.party.name.contains(q, ignoreCase = true) ||
                        (item.party.name?.contains(q, ignoreCase = true) ?: false)
            }
        }

        adapter.submitList(filtered)  // only update recycler items

        // Optional: show "No results" text under search (not full empty layout)
//        binding.tvNoSearchResults.visibility =if (filtered.isEmpty() && q.isNotEmpty()) View.VISIBLE else View.GONE
    }

    /** Update balance summary */
    private fun updateDashboardSummary(totalGiven: Double, totalReceived: Double) {
        val netBalance = totalGiven - totalReceived
        binding.tvBalanceGiven.text = "₹ %.2f".format(totalGiven)
        binding.tvBalanceReceived.text = "₹ %.2f".format(totalReceived)
        binding.tvNetBalance.text = when {
            netBalance > 0 -> "Due ₹ %.2f".format(netBalance)
            netBalance < 0 -> "Advance ₹ %.2f".format(-netBalance)
            else -> "₹ 0.00"
        }
    }

    /** FAB and EmptyView actions */
    private fun setupFabAndEmptyView() {
        val openAddParty = {
            startActivity(
                Intent(this, AddPartyActivity::class.java).putExtra(
                    "ledgerType",
                    viewModel.selectedLedgerType.value?.name
                )
            )
        }
        binding.fabAddParty.setOnClickListener { openAddParty() }
        binding.emptyView.btnAddParty.setOnClickListener { openAddParty() }
    }
}
