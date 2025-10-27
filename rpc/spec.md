# Description

Purpose:
This module implements a hybrid peer-to-peer (P2P) networking node capable of secure message relay and real-time communication using a combination of UDP transport and WebRTC data channels. The design prioritizes low-latency communication, spam-resistance, and end-to-end encryption.

## Core Components:

Node Identity and Key Management:

Each node generates or loads an X25519 key pair for identity and secure message exchange.

Symmetric AES-GCM encryption is derived from a shared secret using HKDF for end-to-end confidentiality.

Node IDs are represented as hexadecimal-encoded public keys.

## Transport Layers:

UDP Layer: Handles asynchronous message delivery with asyncio.DatagramProtocol, supporting both sending and receiving messages.

WebRTC Layer: Managed by RTCNode, establishes peer-to-peer data channels using offers/answers and ICE candidates.

LPC Layer: Local client protocol forwards messages between the local client and the node’s P2P network.

## Messaging System:

Message types include: Announce, RelayOffer, RelayAnswer, RelayCandidate, RelayIntent, Ping, and Pong.

Messages are encrypted per-recipient and include hashcash proof-of-work to prevent replay and spam.

Deduplication is handled with a scalable Bloom filter, periodically reset to manage memory and false-positive rates.

## Peer Management:

Maintains a DHT-like peer list and active connections.

Broadcasts messages to peers while respecting filters to avoid duplicate transmission.

Limits maximum simultaneous WebRTC channels, scaling connections as needed.

Automatic bootstrap from predefined nodes and periodic pinging for liveness.

## Security Features:

End-to-end encryption using ephemeral symmetric keys derived from X25519 key exchange.

AES-GCM provides confidentiality and integrity.

Hashcash proof-of-work prevents spam and mitigates denial-of-service attacks.

Scalable Bloom filters prevent duplicate message processing and replay attacks.

## Connection Management:

rtc_offer and rtc_answer establish WebRTC data channels with peers.

Keep-alive routines ping peers every 10 seconds and detect failed connections.

cancel_connection ensures clean teardown of WebRTC connections and ICE transactions.

## Utilities and Maintenance:

Message hashing, integer conversion, and channel labeling are deterministic to ensure unique channels between peers.

Periodic filter clearing and RTC retry logic maintain network stability over long runtimes.

Relay-only mode allows nodes to act purely as message forwarders without initiating new connections.

## Operational Flow:

Node bootstraps by announcing itself to known peers.

Node listens on UDP for messages, processing intents and WebRTC signals.

Nodes exchange offers and answers to establish encrypted P2P channels.

Messages are broadcasted or routed using data channels and LPC to local clients.

Periodic maintenance tasks ensure network health and connectivity.

## Design Goals:

Low-latency communication via WebRTC data channels.

Decentralized P2P messaging with minimal reliance on central servers.

Secure and private messaging with per-recipient encryption.

Spam-resistant network using hashcash and Bloom filters.

Scalable node operation with automatic peer discovery and channel management.