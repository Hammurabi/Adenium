#pragma once

#include <iostream>
#include <string>
#include <vector>
#include <cstring>
#include <unordered_set>
#include <memory>
#include <arpa/inet.h>
#include <fcntl.h>
#include <unistd.h>
#include <nlohmann/json.hpp>
#include <thread>
#include <mutex>
#include "thread.h"
#include "../util.h"

using json = nlohmann::json;

class ClientConnection {
public:
    int m_Sockfd;
    sockaddr_in m_Addr;
    std::string m_AddrStr;

    ClientConnection(int sock, sockaddr_in addr) : m_Sockfd(sock), m_Addr(addr) {
        char ip[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &m_Addr.sin_addr, ip, INET_ADDRSTRLEN);
        m_AddrStr = std::string(ip) + ":" + std::to_string(ntohs(addr.sin_port));
    }

    ~ClientConnection() {
        close(m_Sockfd);
    }
};

class LPCServer {
public:
    LPCServer(int port) : m_Port(port) {}

    void Start(Thread* thread) {
        m_Sockfd = socket(AF_INET, SOCK_STREAM, 0);
        if (m_Sockfd < 0) {
            perror("Socket creation failed");
            exit(1);
        }

        // Allow address reuse
        int opt = 1;
        setsockopt(m_Sockfd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

        sockaddr_in server_addr{};
        server_addr.sin_family = AF_INET;
        server_addr.sin_addr.s_addr = INADDR_ANY;
        server_addr.sin_port = htons(m_Port);

        if (bind(m_Sockfd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
            perror("Bind failed");
            exit(1);
        }

        if (listen(m_Sockfd, 5) < 0) {
            perror("Listen failed");
            exit(1);
        }

        std::cout << termcolor::yellow << "[*] LPC TCP server listening on port " << m_Port << std::endl;

        AcceptLoop(thread);
    }

    void Submit(const std::string& target, const bytes& data)
    {
        SubReq("Submit", target, data);
    }

    void Request(const std::string& target, const bytes& data)
    {
        SubReq("Request", target, data);
    }

    void BroadcastMessage(const bytes& data)
    {
        json dict;
        dict["Type"] = "Broadcast";
        dict["Content"] = data.hex();
        std::string msg = dict.dump() + "\n";

        std::lock_guard<std::mutex> lock(m_ClientMutex);
        for (auto& client : m_Clients) {
            send(client->m_Sockfd, msg.c_str(), msg.size(), 0);
        }
    }

    void SetIdentity(const std::string& Identity)
    {
        m_Identity = Identity;
        std::cout << termcolor::yellow << "[*] Identity set to " << termcolor::bright_green << m_Identity << std::endl;
    }

private:
    int m_Sockfd;
    int m_Port;
    std::unordered_set<std::shared_ptr<ClientConnection>> m_Clients;
    std::mutex m_ClientMutex;
    std::string m_Identity;

    void SubReq(const std::string& type, const std::string& target, const bytes& data)
    {
        json dict;
        dict["Type"] = type;
        dict["Target"] = target;
        dict["Content"] = data.hex();
        std::string msg = dict.dump() + "\n";

        std::lock_guard<std::mutex> lock(m_ClientMutex);
        for (auto& client : m_Clients) {
            send(client->m_Sockfd, msg.c_str(), msg.size(), 0);
        }
    }

    void AcceptLoop(Thread* thread) {
        while (!thread->Stopped()) {
            sockaddr_in client_addr{};
            socklen_t addr_len = sizeof(client_addr);
            int client_sock = accept(m_Sockfd, (struct sockaddr*)&client_addr, &addr_len);
            if (client_sock < 0) {
                perror("Accept failed");
                continue;
            }

            int flags = fcntl(client_sock, F_GETFL, 0);
            fcntl(client_sock, F_SETFL, flags | O_NONBLOCK);

            auto client = std::make_shared<ClientConnection>(client_sock, client_addr);
            {
                std::lock_guard<std::mutex> lock(m_ClientMutex);
                m_Clients.insert(client);
            }

            std::thread(&LPCServer::ClientLoop, this, client, thread).detach();
            std::cout << termcolor::yellow << "[*] New client connected: " << client->m_AddrStr << std::endl;
        }
    }

    void ClientLoop(std::shared_ptr<ClientConnection> client, Thread* thread) {
        std::string recv_buffer;

        char tmp[4096];
        while (!thread->Stopped()) {
            int n = recv(client->m_Sockfd, tmp, sizeof(tmp), 0);
            if (n > 0) {
                recv_buffer.append(tmp, n);

                size_t pos = 0;
                while ((pos = recv_buffer.find('\n')) != std::string::npos) {
                    std::string message = recv_buffer.substr(0, pos);
                    recv_buffer.erase(0, pos + 1);
                    if (message.empty()) continue;

                    try {
                        std::cout << "Received: " << message << std::endl;
                        json j = json::parse(message);
                        HandleMessage(j, client);
                    } catch (const std::exception& e) {
                        std::cerr << "[!] JSON parse error from " << client->m_AddrStr << ": " << e.what() << std::endl;
                    }
                }

            } else if (n == 0) {
                std::cout << termcolor::yellow << "[-] Client disconnected: " << client->m_AddrStr << std::endl;
                std::lock_guard<std::mutex> lock(m_ClientMutex);
                m_Clients.erase(client);
                break;
            } else {
                if (errno != EWOULDBLOCK && errno != EAGAIN) {
                    perror("recv failed");
                    std::lock_guard<std::mutex> lock(m_ClientMutex);
                    m_Clients.erase(client);
                    break;
                }
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        }
    }

    void HandleMessage(const json& message, std::shared_ptr<ClientConnection> client) {
        std::string type = message.value("Type", "");
        if (type == "Init") {
            SetIdentity(message.value("Content", ""));
        } else if (type == "Message") {
        } else {
            std::cout << termcolor::red << "Received invalid message" << std::endl;
        }
    }
};

