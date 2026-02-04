package net.crewco.Banking

import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import net.crewco.Banking.api.BankingAPI
import net.crewco.Banking.api.BankingAPIProvider
import net.crewco.Banking.commands.*
import net.crewco.Banking.data.database.DatabaseManager
import net.crewco.Banking.data.repositories.*
import net.crewco.Banking.gui.ATMGUI
import net.crewco.Banking.gui.BankGUI
import net.crewco.Banking.gui.CardGUI
import net.crewco.Banking.gui.LoanGUI
import net.crewco.Banking.listeners.ATMInteractListener
import net.crewco.Banking.listeners.PlayerJoinListener
import net.crewco.Banking.services.*
import net.crewco.Banking.util.BankCardItem
import net.crewco.Banking.vault.VaultEconomyProvider
import net.crewco.common.CrewCoPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.ServicePriority

class Startup : CrewCoPlugin() {
    companion object {
        lateinit var plugin: Startup
            private set
        // Database
        lateinit var databaseManager: DatabaseManager
            private set

        // Repositories
        lateinit var accountRepository: AccountRepository
            private set
        lateinit var transactionRepository: TransactionRepository
            private set
        lateinit var cardRepository: CardRepository
            private set
        lateinit var loanRepository: LoanRepository
            private set
        lateinit var atmRepository: ATMRepository
            private set

        // Services
        lateinit var numberGeneratorService: NumberGeneratorService
            private set
        lateinit var transactionService: TransactionService
            private set
        lateinit var accountService: AccountService
            private set
        lateinit var cardService: CardService
            private set
        lateinit var loanService: LoanService
            private set
        lateinit var atmService: ATMService
            private set
        lateinit var interestService: InterestService
            private set

        // API
        lateinit var bankingAPI: BankingAPI
            private set

        // GUI
        lateinit var atmGUI: ATMGUI
            private set

        lateinit var bankGUI: BankGUI
            private set
        lateinit var cardGUI: CardGUI
            private set
        lateinit var loanGUI: LoanGUI
            private set

        // Vault
        private var vaultHooked = false
    }

    override suspend fun onEnableAsync() {
        super.onEnableAsync()

        plugin = this

        // Config
        saveDefaultConfig()

        // Database
        databaseManager = DatabaseManager(this)
        databaseManager.connect()

        // Initialize services (these must complete before Vault registration)
        initializeServices()

        // Initialize API
        initializeAPI()

        // Register Vault - IMPORTANT: Do this synchronously on the main thread
        // Schedule it to run on next tick to ensure we're on the main thread
        // and all services are fully initialized
        server.scheduler.runTask(this, Runnable {
            setupVault()
        })

        // Register commands
        registerCommands(
            BankCommand::class,
            CardCommand::class,
            LoanCommand::class,
            ATMCommand::class,
            AdminBankCommand::class
        )

        // Register listeners
        registerListeners(
            PlayerJoinListener::class,
            ATMInteractListener::class
        )

        BankCardItem.initialize(this)

        // Start scheduled tasks
        interestService.startScheduler()

        logger.info("CrewCo Banking enabled!")
    }

    private fun initializeServices() {
        // Repositories
        accountRepository = AccountRepository(databaseManager)
        transactionRepository = TransactionRepository(databaseManager)
        cardRepository = CardRepository(databaseManager)
        loanRepository = LoanRepository(databaseManager)
        atmRepository = ATMRepository(databaseManager, this)

        // Services (order matters due to dependencies)
        numberGeneratorService = NumberGeneratorService()
        transactionService = TransactionService(transactionRepository, numberGeneratorService)
        accountService = AccountService(accountRepository, numberGeneratorService, transactionService)
        cardService = CardService(cardRepository, accountRepository, numberGeneratorService, transactionService)
        loanService = LoanService(loanRepository, accountRepository, numberGeneratorService, transactionService)
        atmService = ATMService(atmRepository, accountRepository, numberGeneratorService, transactionService)
        interestService = InterestService(this, accountRepository, transactionService)

        // GUI
        atmGUI = ATMGUI(this)
        bankGUI = BankGUI(this)
        cardGUI = CardGUI(this)
        loanGUI = LoanGUI(this)
    }

    private fun initializeAPI() {
        bankingAPI = BankingAPIProvider(
            accountService,
            cardService,
            loanService,
            transactionService,
            atmService,
            numberGeneratorService
        )

        // Register as Bukkit service
        server.servicesManager.register(
            BankingAPI::class.java,
            bankingAPI,
            this,
            ServicePriority.Normal
        )

        logger.info("Banking API registered!")
    }

    private fun setupVault() {
        val vaultPlugin = server.pluginManager.getPlugin("Vault")
        if (vaultPlugin == null) {
            logger.warning("Vault not found! Economy integration disabled.")
            return
        }

        if (!vaultPlugin.isEnabled) {
            logger.warning("Vault is not enabled! Economy integration disabled.")
            return
        }

        try {
            val economyProvider = VaultEconomyProvider(this, accountService)

            server.servicesManager.register(
                Economy::class.java,
                economyProvider,
                this,
                ServicePriority.Highest
            )

            // Verify registration
            val registeredProvider = server.servicesManager.getRegistration(Economy::class.java)
            if (registeredProvider != null && registeredProvider.provider is VaultEconomyProvider) {
                vaultHooked = true
                logger.info("Vault economy provider registered successfully!")
                logger.info("Economy provider: ${registeredProvider.provider.name}")
            } else {
                logger.warning("Vault economy registration may have been overridden by another plugin!")
                if (registeredProvider != null) {
                    logger.warning("Current provider: ${registeredProvider.provider.name} from ${registeredProvider.plugin.name}")
                }
            }
        } catch (e: Exception) {
            logger.severe("Failed to register Vault economy provider: ${e.message}")
            e.printStackTrace()
        }
    }

    fun isVaultHooked(): Boolean = vaultHooked

    override suspend fun onDisableAsync() {
        super.onDisableAsync()

        // Unregister Vault provider
        if (vaultHooked) {
            server.servicesManager.unregisterAll(this)
        }

        // Disconnect database
        if (databaseManager.isConnected()) {
            databaseManager.disconnect()
        }

        logger.info("CrewCo Banking disabled!")
    }
}