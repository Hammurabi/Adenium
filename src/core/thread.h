#pragma once
#include <iostream>
#include <thread>
#include <atomic>
#include <condition_variable>
#include <functional>
#include <string>
#include "ikalnytskyi/termcolor.hpp"

class Thread
{
public:
    Thread(const std::string& name, const std::function<void()>& fn)
        : m_StopFlag(false), m_ThreadName(name)
    {
        m_Worker = std::thread([this, fn] {
            fn();
            std::cout << termcolor::red << m_ThreadName << " Thread Terminated" << std::endl;
        });
    }

    ~Thread() {
        Stop();
        Join();
    }

    void Stop() {
        m_StopFlag.store(true);
        m_Cv.notify_all();
    }

    bool Stopped() const {
        return m_StopFlag.load();
    }

    void Join() {
        if (m_Worker.joinable()) m_Worker.join();
    }

    bool Wait() {
        std::unique_lock<std::mutex> lock(m_Mutex);
        m_Cv.wait(lock, [this]{ return m_StopFlag.load(); });
        return Stopped();
    }

    bool WaitFor(std::chrono::milliseconds TimeOut) {
        std::unique_lock<std::mutex> lock(m_Mutex);
        return m_Cv.wait_for(lock, TimeOut, [this]{ return m_StopFlag.load(); });
    }

    void Detach() {
        m_Worker.detach();
    }
private:
    std::atomic<bool> m_StopFlag;
    std::condition_variable m_Cv;
    std::thread m_Worker;
    std::mutex  m_Mutex;
    std::string m_ThreadName;
};
