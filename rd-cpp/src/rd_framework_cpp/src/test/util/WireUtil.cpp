//
// Created by jetbrains on 19.09.2018.
//

#include "demangle.h"
#include "WireUtil.h"
#include "PassiveSocket.h"
#include "Host.h"

#include <utility>
#include <thread>

uint16_t find_free_port() {
    CPassiveSocket fake_server;
    fake_server.Initialize();
    fake_server.Listen("127.0.0.1", 0);
	uint16_t port = fake_server.GetServerPort();
    MY_ASSERT_MSG(port != 0, "no free port");
    return port;
}

void sleep_this_thread(int ms) {
    std::this_thread::sleep_for(std::chrono::milliseconds(ms));
}
