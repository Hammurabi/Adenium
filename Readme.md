# 🌸 Adenium 1/32 🌸
Welcome to Adenium

Adenium is a next-generation blockchain network designed to be fast, secure, and reliable. It allows developers to build decentralized applications and services in a way that’s deterministic, everything behaves predictably and provable, so you can always verify that what happened on the network.

🚀 Lightning-Fast Transactions

Transactions are tiny—just 4-25 bytes per block (smaller than a hash!) 📝

With 192 shards, Adenium can process between 41,943,040 to 6,710,886 transactions per slot ⚡ (≈ 3,495,253–559,240 TPS!)

🛡️ Safety First

Separation of concerns: Accounts 👤 and Deployments 🖥️ are inherently separate, preventing accidental or malicious siphoning of funds.

Deployments are live programs that can automate tasks, interact with other deployments, and execute safely without touching user accounts.

🌐 Why Adenium Rocks

Deterministic & provable: ✅ predictable and verifiable results

Efficient & scalable: 📈 small transaction sizes and smart shard architecture

Developer-friendly: 🛠️ Deployments behave like mini servers with security built-in

Collaborative validation: 🤝 validators work together instead of wasting energy

Adenium is designed to make Web3 applications fast, safe, and scalable—a network ready for BILLIONS of users and deployments 🚀💎.

Discord: https://discord.gg/wqt5FZGbQe

---

## ⚡ Getting Started with Adenium

### 🐍 Gateway Node

The Gateway node allows you to connect to the network easily.

* **Python ≥ 9** is required.
* Basic familiarity with command-line operations is helpful.

### 🏃‍♂️ Steps to Run a Full Node

1. **Navigate to the GATEWAY folder**

   ```bash
   cd gateway
   ```

2. **(Optional but recommended) Create a Python virtual environment**

   ```bash
   python -m venv adn
   source adn/bin/activate  # Linux / macOS
   adn\Scripts\activate     # Windows
   ```

3. **Install dependencies**

   ```bash
   pip install -r requirements.txt
   ```

4. **Run the node**

   ```bash
   python main.py
   ```

### 🔧 Node Options

* **Relay-only mode**

  ```bash
  python main.py relay_only
  ```

  Your node will connect only via its public IP, communicate with bootstrap nodes, and relay surface messages. It will not attempt NAT traversal or find peers via DHT keys.

* **Verbose mode**

  ```bash
  python main.py verbose
  ```

  Prints detailed information to the console for monitoring and debugging.

---


## 💖 Support / Fund Me

If you like Adenium and want to support development, you can send funds to the following wallets:

🌐 Ethereum: 0x64b699e0cff956e39f473CdfeA74013a46C421dc

₿ Bitcoin: 0x64b699e0cff956e39f473CdfeA74013a46C421dc

☀️ Solana: ARg9Kp4JHPou8yPVkDs4iaFxznq2tmBUXzVH6TGXwJ8N