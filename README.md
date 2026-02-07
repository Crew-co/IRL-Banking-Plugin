# CrewCo Banking Plugin

A comprehensive, realistic banking system for Minecraft servers featuring physical currency, player-owned banks, ATMs, loans, cards, and a central Treasury system.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start Guide](#quick-start-guide)
- [Physical Money System](#physical-money-system)
- [Account Types](#account-types)
- [Cards](#cards)
- [ATM System](#atm-system)
- [Player-Owned Banks](#player-owned-banks)
- [Treasury System](#treasury-system)
- [Loans](#loans)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [API for Developers](#api-for-developers)
- [Vault Integration](#vault-integration)

---

## Features

### Core Banking
- **Multiple Account Types**: Wallet, Checking, Savings, Business, and Investment accounts
- **Realistic Account Numbers**: Auto-generated account and routing numbers
- **Interest System**: Configurable interest rates for savings accounts
- **Transaction History**: Full audit trail of all transactions

### Physical Money
- **14 Currency Denominations**: From $0.01 pennies to $100 bills
- **Craftable Money Items**: Physical items players can hold, trade, and store
- **Wallet System**: Player's primary account acts as cash-on-hand
- **Money Collection**: Pick up money items automatically or manually

### Cards & ATMs
- **Debit & Credit Cards**: Physical card items with card numbers, CVV, expiration
- **ATM Network**: Place ATMs around your server
- **Full ATM GUI**: Withdraw, deposit, transfer, check balance, manage cards/loans
- **Out-of-Network Fees**: Non-members pay extra at bank-owned ATMs

### Player-Owned Banks
- **Start Your Own Bank**: Players can create and operate their own banks
- **Set Custom Rates**: Control interest rates, fees, and limits
- **Manage Reserves**: Realistic reserve requirements
- **Deploy ATMs**: Place ATMs that earn fees for your bank
- **Customer Management**: Track members and transactions

### Central Treasury
- **Monetary Policy**: Server admins control the economy
- **Bank Licensing**: Regulate who can open banks
- **Reserve Requirements**: Set minimum reserve ratios
- **Rate Caps**: Control maximum interest rates and fees
- **Economic Reports**: Monitor money supply and bank health

### Loans
- **Multiple Loan Types**: Personal, Business, Mortgage, Auto, Student, Emergency
- **Realistic Terms**: Principal, interest, monthly payments, term length
- **Loan Management**: Apply, approve, pay off through ATM GUI

---

## Requirements

- **Minecraft Server**: Paper/Spigot 1.20+
- **Java**: 17 or higher
- **Dependencies**:
    - [Vault](https://www.spigotmc.org/resources/vault.34315/) (optional, for economy integration)
    - [InventoryFramework](https://github.com/stefvanschie/IF) (included)
    - MCCoroutine (included)

---

## Installation

1. Download the latest release JAR
2. Place in your server's `plugins/` folder
3. Start/restart the server
4. Configure `plugins/CrewCoBanking/config.yml` as needed
5. Give players appropriate permissions

---

## Quick Start Guide

### For Players

1. **Join the server** - A wallet is automatically created for you
2. **Check your balance**: `/bank` or `/bank balance`
3. **Open additional accounts**: `/bank open savings MyAccount`
4. **Get a card**: `/card request debit <account>`
5. **Use an ATM**: Right-click an ATM sign while holding your card
6. **Pick up money**: Physical money items add to your wallet automatically

### For Server Admins

1. **Create system ATMs**: `/atm create` while looking at an iron block with [ATM] sign
2. **Give starting money**: `/adminbank give <player> <amount>`
3. **Configure Treasury**: `/treasury` commands to set economic policy
4. **Monitor economy**: `/treasury report`

---

## Physical Money System

### Denominations

| Item | Value | Material |
|------|-------|----------|
| Penny | $0.01 | Copper Ingot |
| Nickel | $0.05 | Iron Nugget |
| Dime | $0.10 | Iron Nugget |
| Quarter | $0.25 | Iron Nugget |
| Half Dollar | $0.50 | Iron Nugget |
| Dollar Coin | $1.00 | Gold Nugget |
| $1 Bill | $1.00 | Paper |
| $2 Bill | $2.00 | Paper |
| $5 Bill | $5.00 | Paper |
| $10 Bill | $10.00 | Paper |
| $20 Bill | $20.00 | Paper |
| $50 Bill | $50.00 | Paper |
| $100 Bill | $100.00 | Paper |

### Money Commands

```
/money                    - Check wallet balance
/money pickup [on|off]    - Toggle auto-pickup of money items
/money collect            - Collect all money in inventory to wallet
/money withdraw <amount>  - Get physical money from wallet
/money deposit            - Deposit held money to wallet
```

### How It Works

- Your **wallet** is your cash-on-hand balance
- Physical money items represent actual currency
- Picking up money adds to your wallet
- ATM withdrawals give you physical money AND update your wallet
- ATM deposits take physical money AND update your accounts

---

## Account Types

| Type | Description | Interest | Use Case |
|------|-------------|----------|----------|
| **Wallet** | Primary account, auto-created | None | Cash on hand |
| **Checking** | Daily transactions | Low | Regular spending |
| **Savings** | Earns interest | High | Long-term savings |
| **Business** | For shops/businesses | Medium | Business operations |
| **Investment** | High interest, limited access | Highest | Investing |

### Account Commands

```
/bank                           - List all your accounts
/bank balance [account#]        - Check balance
/bank open <type> [name]        - Open new account
/bank close <account#>          - Close an account
/bank transfer <from> <to> <$>  - Transfer between accounts
/bank send <player> <amount>    - Send money to player
/bank history [account#]        - View transactions
/bank rename <account#> <name>  - Rename account
```

---

## Cards

### Card Types

| Type | Description |
|------|-------------|
| **Debit** | Linked to account, direct withdrawal |
| **Credit** | Credit line (requires approval) |
| **ATM** | ATM-only access |
| **Premium** | Higher limits |
| **Business** | For business accounts |

### Card Commands

```
/card list                      - List your cards
/card request <type> <account#> - Request a new card
/card info <card#>              - View card details
/card freeze <card#>            - Freeze a card
/card unfreeze <card#>          - Unfreeze a card
/card setpin <card#> <pin>      - Set card PIN
/card cancel <card#>            - Cancel a card
```

### Physical Card Item

- Cards are actual items you hold
- Right-click an ATM while holding a card to use it
- Cards display: Type, masked number, linked account

---

## ATM System

### Creating ATMs

**Server ATMs (Admin)**:
```
/atm create              - Create ATM at target block
/atm remove <id>         - Remove an ATM
/atm list                - List all ATMs
/atm refill <id> <$>     - Add cash to ATM
/atm info <id>           - View ATM details
```

**Bank-Owned ATMs (Bank Owners)**:
```
/mybank atm place <tag>                    - Place ATM for your bank
/mybank atm list <tag>                     - List your ATMs
/mybank atm setfee <tag> <id> <fee> <non>  - Set fees
/mybank atm refill <tag> <id> <amount>     - Refill from reserves
/mybank atm remove <tag> <id>              - Remove ATM
```

### ATM Structure

ATMs are built by players:
1. Place a valid base block (Iron Block, Quartz, Polished Stone variants)
2. Attach a wall sign to the block
3. Write `[ATM]` on the first line
4. Register the ATM via commands

### ATM Features (GUI)

When you use an ATM with a card:
- **Check Balance** - View account balance
- **Withdraw Cash** - Get physical money
- **Deposit Cash** - Deposit wallet cash to account
- **Send Money** - Transfer to accounts or players
- **My Accounts** - View/manage all accounts
- **My Cards** - View/manage cards
- **My Loans** - View loans, make payments
- **Transaction History** - Recent transactions
- **Apply for Loan** - Request new loans

### Fee Structure

| ATM Type | Member Fee | Non-Member Fee |
|----------|------------|----------------|
| System | Base fee | Base fee |
| Bank-Owned | Base fee | Base fee + Out-of-network fee |

---

## Player-Owned Banks

### Starting a Bank

```
/mybank create <name> <tag>
```

- Costs a license fee (set by Treasury)
- Tag is a short identifier (e.g., "FCB" for First City Bank)
- You become the bank owner

### Managing Your Bank

```
/mybank                         - List your banks
/mybank info <tag>              - Detailed bank info
/mybank open <tag>              - Open for customers
/mybank close <tag>             - Close to new customers
/mybank rename <tag> <newname>  - Rename bank
```

### Setting Rates & Fees

```
/mybank setrate <tag> savings <rate>     - Savings interest rate
/mybank setrate <tag> loan <rate>        - Loan interest rate
/mybank setfee <tag> atm <withdraw> <deposit>
/mybank setfee <tag> transfer <fee>
/mybank setfee <tag> account <fee>       - Account opening fee
```

### Managing Reserves

```
/mybank deposit <tag> <amount>   - Add to reserves
/mybank withdraw <tag> <amount>  - Take from reserves
```

Banks must maintain minimum reserves (set by Treasury).

### Customer Commands

Players can join banks:
```
/banks                          - List all available banks
/banks info <tag>               - View bank details
/banks join <tag>               - Join a bank
/banks leave <tag>              - Leave a bank
/banks memberships              - View your memberships
```

---

## Treasury System

The Treasury is the central bank controlled by server admins.

### Treasury Commands

```
/treasury                       - View Treasury status
/treasury report                - Economic report
```

### Monetary Policy

```
/treasury setrate reserve <rate>     - Reserve requirement (0.0-1.0)
/treasury setrate interest <rate>    - Base interest rate
/treasury setfee license <amount>    - Bank license cost
/treasury setfee atmlicense <amount> - ATM license cost
/treasury setlimit atm <amount>      - Max ATM fee allowed
/treasury setlimit atmsperbank <n>   - Max ATMs per bank
```

### Bank Regulation

```
/treasury freeze <bankId>       - Freeze a bank
/treasury unfreeze <bankId>     - Unfreeze a bank
/treasury suspend <bankId>      - Suspend a bank
/treasury unsuspend <bankId>    - Unsuspend a bank
/treasury audit <bankId>        - Audit a bank
/treasury banks                 - List all banks
```

### Money Supply

```
/treasury print <amount>        - Print new money (inflation)
/treasury destroy <amount>      - Remove money (deflation)
/treasury supply                - View current money supply
```

---

## Loans

### Loan Types

| Type | Max Amount | Max Term | Base Rate | Collateral |
|------|------------|----------|-----------|------------|
| Personal | $10,000 | 24 months | 8.5% | No |
| Business | $100,000 | 60 months | 6.5% | Yes |
| Mortgage | $500,000 | 360 months | 4.5% | Yes |
| Auto | $50,000 | 72 months | 5.5% | Yes |
| Student | $75,000 | 120 months | 4.0% | No |
| Emergency | $5,000 | 12 months | 12.0% | No |

### Loan Commands

```
/loan list                      - View your loans
/loan info <loanId>             - Loan details
/loan apply <type> <amount> <months> <account#>
/loan pay <loanId> <amount>     - Make a payment
/loan payoff <loanId>           - Pay off entire loan
/loan types                     - View available loan types
```

### Admin Loan Commands

```
/loan admin list                - All pending loans
/loan admin approve <loanId>    - Approve a loan
/loan admin deny <loanId>       - Deny a loan
/loan admin forgive <loanId>    - Forgive a loan
```

---

## Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bank` | List accounts | `banking.use` |
| `/bank balance` | Check balance | `banking.use` |
| `/bank open` | Open account | `banking.open` |
| `/bank close` | Close account | `banking.close` |
| `/bank transfer` | Transfer money | `banking.transfer` |
| `/bank send` | Send to player | `banking.send` |
| `/card` | Card management | `banking.cards` |
| `/loan` | Loan management | `banking.loans` |
| `/money` | Money management | `banking.money` |
| `/banks` | Browse banks | `banking.banks` |
| `/mybank` | Manage your bank | `banking.bank.*` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/adminbank give` | Give money | `banking.admin` |
| `/adminbank take` | Take money | `banking.admin` |
| `/adminbank set` | Set balance | `banking.admin` |
| `/adminbank freeze` | Freeze account | `banking.admin` |
| `/adminbank lookup` | Look up player | `banking.admin` |
| `/atm create` | Create ATM | `banking.admin.atm` |
| `/atm remove` | Remove ATM | `banking.admin.atm` |
| `/treasury` | Treasury controls | `banking.treasury` |

---

## Permissions

### Basic Permissions

```yaml
banking.use          - Basic banking access
banking.open         - Open new accounts
banking.close        - Close accounts
banking.transfer     - Transfer between accounts
banking.send         - Send money to players
banking.cards        - Use card system
banking.loans        - Use loan system
banking.money        - Use physical money
```

### Bank Owner Permissions

```yaml
banking.bank.create  - Create a bank
banking.bank.list    - List your banks
banking.bank.info    - View bank info
banking.bank.settings - Change rates/fees
banking.bank.reserves - Manage reserves
banking.bank.status  - Open/close bank
banking.bank.atm     - Manage bank ATMs
```

### Admin Permissions

```yaml
banking.admin        - All admin commands
banking.admin.atm    - ATM management
banking.treasury     - Treasury controls
banking.loan.admin   - Loan administration
```

### Default Permission Groups

```yaml
# Example for LuckPerms
default:
  - banking.use
  - banking.open
  - banking.transfer
  - banking.send
  - banking.cards
  - banking.loans
  - banking.money
  - banking.banks
  - banking.bank.create
  - banking.bank.list
  - banking.bank.info
  - banking.bank.settings
  - banking.bank.reserves
  - banking.bank.status
  - banking.bank.atm

admin:
  - banking.admin
  - banking.admin.atm
  - banking.treasury
  - banking.loan.admin
```

---

## Configuration

### config.yml

```yaml
# Database settings
database:
  type: sqlite  # sqlite or mysql
  file: banking.db
  
# MySQL settings (if type is mysql)
mysql:
  host: localhost
  port: 3306
  database: banking
  username: root
  password: ""

# Starting balance for new players
starting-balance: 100.0

# Interest settings
interest:
  enabled: true
  interval: 3600  # seconds between interest payments
  
# Money settings
money:
  auto-pickup: true
  pickup-radius: 2.0

# ATM settings
atm:
  default-cash: 100000.0
  default-max-withdrawal: 5000.0
  default-fee: 2.50
  default-out-of-network-fee: 5.00

# Treasury defaults
treasury:
  reserve-requirement: 0.10
  base-interest-rate: 2.0
  bank-license-cost: 10000.0
  atm-license-cost: 1000.0
  max-atm-fee: 10.0
  max-atms-per-bank: 20
```

---

## API for Developers

### Getting the API

```kotlin
val bankingAPI = server.servicesManager.getRegistration(BankingAPI::class.java)?.provider
```

### API Methods

```kotlin
interface BankingAPI {
    // Accounts
    suspend fun getAccount(accountNumber: String): BankAccount?
    suspend fun getPlayerAccounts(playerUuid: UUID): List<BankAccount>
    suspend fun createAccount(playerUuid: UUID, type: AccountType, name: String): BankAccount?
    suspend fun getBalance(accountNumber: String): Double?
    suspend fun deposit(accountNumber: String, amount: Double, initiator: UUID, description: String): Boolean
    suspend fun withdraw(accountNumber: String, amount: Double, initiator: UUID, description: String): Boolean
    suspend fun transfer(from: String, to: String, amount: Double, initiator: UUID, description: String): Boolean
    
    // Cards
    suspend fun getCard(cardNumber: String): Card?
    suspend fun getPlayerCards(playerUuid: UUID): List<Card>
    suspend fun createCard(playerUuid: UUID, accountNumber: String, type: CardType): Card?
    
    // Loans
    suspend fun getLoan(loanId: String): Loan?
    suspend fun getPlayerLoans(playerUuid: UUID): List<Loan>
    suspend fun createLoan(playerUuid: UUID, type: LoanType, amount: Double, term: Int, account: String): Loan?
    
    // ATMs
    suspend fun getATM(atmId: String): ATM?
    suspend fun getNearbyATMs(location: Location, radius: Double): List<ATM>
    
    // Transactions
    suspend fun getTransactionHistory(accountNumber: String, limit: Int): List<Transaction>
}
```

### Example Usage

```kotlin
class MyPlugin : JavaPlugin() {
    private var bankingAPI: BankingAPI? = null
    
    override fun onEnable() {
        // Get API after CrewCoBanking is loaded
        server.scheduler.runTaskLater(this, Runnable {
            bankingAPI = server.servicesManager
                .getRegistration(BankingAPI::class.java)?.provider
            
            if (bankingAPI != null) {
                logger.info("Connected to CrewCo Banking API!")
            }
        }, 20L)
    }
    
    fun giveReward(player: Player, amount: Double) {
        val api = bankingAPI ?: return
        
        // Run in coroutine context
        plugin.launch {
            val accounts = api.getPlayerAccounts(player.uniqueId)
            val wallet = accounts.find { it.accountType == AccountType.WALLET }
            
            if (wallet != null) {
                api.deposit(wallet.accountNumber, amount, player.uniqueId, "Quest Reward")
                player.sendMessage("You received $$amount!")
            }
        }
    }
}
```

---

## Vault Integration

CrewCo Banking registers as a Vault economy provider automatically.

### For Other Plugins

Any plugin that uses Vault economy will work with CrewCo Banking:

```java
// Standard Vault usage works
Economy econ = getServer().getServicesManager()
    .getRegistration(Economy.class).getProvider();

econ.depositPlayer(player, 100.0);  // Adds to wallet
econ.withdrawPlayer(player, 50.0);  // Takes from wallet
econ.getBalance(player);            // Returns wallet balance
```

### Notes

- Vault operations use the player's **Wallet** account
- Physical money items are given/taken automatically
- All Vault transactions are logged

---

## Support

- **Issues**: Report bugs on GitHub
- **Discord**: Join our community server
- **Wiki**: Extended documentation available

---

## License

This plugin is proprietary software. All rights reserved.

---

## Credits

- **Development**: CrewCo Development Team
- **Libraries**:
    - [Adventure API](https://docs.adventure.kyori.net/)
    - [InventoryFramework](https://github.com/stefvanschie/IF)
    - [MCCoroutine](https://github.com/Shynixn/MCCoroutine)
    - [Cloud Command Framework](https://github.com/Incendo/cloud)
