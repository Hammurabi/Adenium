import os
import sys
import json
import math
import time
import struct
import random
import hashlib
import asyncio
import secrets
import requests
import colorama
import traceback
from bitarray import bitarray
from cryptography.hazmat.primitives.asymmetric import x25519
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from aiortc import RTCPeerConnection, RTCSessionDescription, RTCDataChannel, RTCIceCandidate, RTCConfiguration, RTCIceServer

public_ip = requests.get('https://api.ipify.org').text
listen_on = ('0.0.0.0', 1152)
lpc_from = ('0.0.0.0', 2304)
lpc_to = ('127.0.0.1', 3456)
verbose = False
bootstrap_nodes = [
    ('204.44.125.165', 1152),
]


stun_servers = [
    {'urls': 'stun:stun.voipstunt.com'},
    {'urls': 'stun:stun.ekiga.net'},

    {'urls': 'stun:stun.l.google.com:19302'},
    {'urls': 'stun:stun1.l.google.com:19302'},
    {'urls': 'stun:stun2.l.google.com:19302'},
    {'urls': 'stun:stun3.l.google.com:19302'},
    {'urls': 'stun:stun4.l.google.com:19302'},
    {'urls': 'stun:stun.services.mozilla.com'},
    {'urls': 'stun:numb.viagenie.ca'},
    {'urls': 'stun:stun.stunprotocol.org:3478'},

    {'urls': 'stun:stun.ideasip.com'},
    {'urls': 'stun:stun.rixtelecom.se'},
    {'urls': 'stun:stun.schlund.de'},
    {'urls': 'stun:stun.stunprotocol.org:3478'},
    {'urls': 'stun:stun.voiparound.com'},
    {'urls': 'stun:stun.voipbuster.com'},
    {'urls': 'stun:stun.voxgratia.org'},
    {'urls': 'stun:23.21.150.121:3478'},
    {'urls': 'stun:iphone-stun.strato-iphone.de:3478'},
    {'urls': 'stun:numb.viagenie.ca:3478'},
    {'urls': 'stun:s1.taraba.net:3478'},
    {'urls': 'stun:s2.taraba.net:3478'},
    {'urls': 'stun:stun.12connect.com:3478'},
    {'urls': 'stun:stun.12voip.com:3478'},
    {'urls': 'stun:stun.1und1.de:3478'},
    {'urls': 'stun:stun.2talk.co.nz:3478'},
    {'urls': 'stun:stun.2talk.com:3478'},
    {'urls': 'stun:stun.3clogic.com:3478'},
    {'urls': 'stun:stun.3cx.com:3478'},
    {'urls': 'stun:stun.a-mm.tv:3478'},
    {'urls': 'stun:stun.aa.net.uk:3478'},
    {'urls': 'stun:stun.acrobits.cz:3478'},
    {'urls': 'stun:stun.actionvoip.com:3478'},
    {'urls': 'stun:stun.advfn.com:3478'},
    {'urls': 'stun:stun.aeta-audio.com:3478'},
    {'urls': 'stun:stun.aeta.com:3478'},
    {'urls': 'stun:stun.alltel.com.au:3478'},
    {'urls': 'stun:stun.altar.com.pl:3478'},
    {'urls': 'stun:stun.annatel.net:3478'},
    {'urls': 'stun:stun.antisip.com:3478'},
    {'urls': 'stun:stun.arbuz.ru:3478'},
    {'urls': 'stun:stun.avigora.com:3478'},
    {'urls': 'stun:stun.avigora.fr:3478'},
    {'urls': 'stun:stun.awa-shima.com:3478'},
    {'urls': 'stun:stun.awt.be:3478'},
    {'urls': 'stun:stun.b2b2c.ca:3478'},
    {'urls': 'stun:stun.bahnhof.net:3478'},
    {'urls': 'stun:stun.barracuda.com:3478'},
    {'urls': 'stun:stun.bluesip.net:3478'},
    {'urls': 'stun:stun.bmwgs.cz:3478'},
    {'urls': 'stun:stun.botonakis.com:3478'},
    {'urls': 'stun:stun.budgetphone.nl:3478'},
    {'urls': 'stun:stun.budgetsip.com:3478'},
    {'urls': 'stun:stun.cablenet-as.net:3478'},
    {'urls': 'stun:stun.callromania.ro:3478'},
    {'urls': 'stun:stun.callwithus.com:3478'},
    {'urls': 'stun:stun.cbsys.net:3478'},
    {'urls': 'stun:stun.chathelp.ru:3478'},
    {'urls': 'stun:stun.cheapvoip.com:3478'},
    {'urls': 'stun:stun.ciktel.com:3478'},
    {'urls': 'stun:stun.cloopen.com:3478'},
    {'urls': 'stun:stun.colouredlines.com.au:3478'},
    {'urls': 'stun:stun.comfi.com:3478'},
    {'urls': 'stun:stun.commpeak.com:3478'},
    {'urls': 'stun:stun.comtube.com:3478'},
    {'urls': 'stun:stun.comtube.ru:3478'},
    {'urls': 'stun:stun.cope.es:3478'},
    {'urls': 'stun:stun.counterpath.com:3478'},
    {'urls': 'stun:stun.counterpath.net:3478'},
    {'urls': 'stun:stun.cryptonit.net:3478'},
    {'urls': 'stun:stun.darioflaccovio.it:3478'},
    {'urls': 'stun:stun.datamanagement.it:3478'},
    {'urls': 'stun:stun.dcalling.de:3478'},
    {'urls': 'stun:stun.decanet.fr:3478'},
    {'urls': 'stun:stun.demos.ru:3478'},
    {'urls': 'stun:stun.develz.org:3478'},
    {'urls': 'stun:stun.dingaling.ca:3478'},
    {'urls': 'stun:stun.doublerobotics.com:3478'},
    {'urls': 'stun:stun.drogon.net:3478'},
    {'urls': 'stun:stun.duocom.es:3478'},
    {'urls': 'stun:stun.dus.net:3478'},
    {'urls': 'stun:stun.e-fon.ch:3478'},
    {'urls': 'stun:stun.easybell.de:3478'},
    {'urls': 'stun:stun.easycall.pl:3478'},
    {'urls': 'stun:stun.easyvoip.com:3478'},
    {'urls': 'stun:stun.efficace-factory.com:3478'},
    {'urls': 'stun:stun.einsundeins.com:3478'},
    {'urls': 'stun:stun.einsundeins.de:3478'},
    {'urls': 'stun:stun.ekiga.net:3478'},
    {'urls': 'stun:stun.epygi.com:3478'},
    {'urls': 'stun:stun.etoilediese.fr:3478'},
    {'urls': 'stun:stun.eyeball.com:3478'},
    {'urls': 'stun:stun.faktortel.com.au:3478'},
    {'urls': 'stun:stun.freecall.com:3478'},
    {'urls': 'stun:stun.freeswitch.org:3478'},
    {'urls': 'stun:stun.freevoipdeal.com:3478'},
    {'urls': 'stun:stun.fuzemeeting.com:3478'},
    {'urls': 'stun:stun.gmx.de:3478'},
    {'urls': 'stun:stun.gmx.net:3478'},
    {'urls': 'stun:stun.gradwell.com:3478'},
    {'urls': 'stun:stun.halonet.pl:3478'},
    {'urls': 'stun:stun.hellonanu.com:3478'},
    {'urls': 'stun:stun.hoiio.com:3478'},
    {'urls': 'stun:stun.hosteurope.de:3478'},
    {'urls': 'stun:stun.ideasip.com:3478'},
    {'urls': 'stun:stun.imesh.com:3478'},
    {'urls': 'stun:stun.infra.net:3478'},
    {'urls': 'stun:stun.internetcalls.com:3478'},
    {'urls': 'stun:stun.intervoip.com:3478'},
    {'urls': 'stun:stun.ipcomms.net:3478'},
    {'urls': 'stun:stun.ipfire.org:3478'},
    {'urls': 'stun:stun.ippi.fr:3478'},
    {'urls': 'stun:stun.ipshka.com:3478'},
    {'urls': 'stun:stun.iptel.org:3478'},
    {'urls': 'stun:stun.irian.at:3478'},
    {'urls': 'stun:stun.it1.hr:3478'},
    {'urls': 'stun:stun.ivao.aero:3478'},
    {'urls': 'stun:stun.jappix.com:3478'},
    {'urls': 'stun:stun.jumblo.com:3478'},
    {'urls': 'stun:stun.justvoip.com:3478'},
    {'urls': 'stun:stun.kanet.ru:3478'},
    {'urls': 'stun:stun.kiwilink.co.nz:3478'},
    {'urls': 'stun:stun.kundenserver.de:3478'},
    {'urls': 'stun:stun.linea7.net:3478'},
    {'urls': 'stun:stun.linphone.org:3478'},
    {'urls': 'stun:stun.liveo.fr:3478'},
    {'urls': 'stun:stun.lowratevoip.com:3478'},
    {'urls': 'stun:stun.lugosoft.com:3478'},
    {'urls': 'stun:stun.lundimatin.fr:3478'},
    {'urls': 'stun:stun.magnet.ie:3478'},
    {'urls': 'stun:stun.manle.com:3478'},
    {'urls': 'stun:stun.mgn.ru:3478'},
    {'urls': 'stun:stun.mit.de:3478'},
    {'urls': 'stun:stun.mitake.com.tw:3478'},
    {'urls': 'stun:stun.miwifi.com:3478'},
    {'urls': 'stun:stun.modulus.gr:3478'},
    {'urls': 'stun:stun.mozcom.com:3478'},
    {'urls': 'stun:stun.myvoiptraffic.com:3478'},
    {'urls': 'stun:stun.mywatson.it:3478'},
    {'urls': 'stun:stun.nas.net:3478'},
    {'urls': 'stun:stun.neotel.co.za:3478'},
    {'urls': 'stun:stun.netappel.com:3478'},
    {'urls': 'stun:stun.netappel.fr:3478'},
    {'urls': 'stun:stun.netgsm.com.tr:3478'},
    {'urls': 'stun:stun.nfon.net:3478'},
    {'urls': 'stun:stun.noblogs.org:3478'},
    {'urls': 'stun:stun.noc.ams-ix.net:3478'},
    {'urls': 'stun:stun.node4.co.uk:3478'},
    {'urls': 'stun:stun.nonoh.net:3478'},
    {'urls': 'stun:stun.nottingham.ac.uk:3478'},
    {'urls': 'stun:stun.nova.is:3478'},
    {'urls': 'stun:stun.nventure.com:3478'},
    {'urls': 'stun:stun.on.net.mk:3478'},
    {'urls': 'stun:stun.ooma.com:3478'},
    {'urls': 'stun:stun.ooonet.ru:3478'},
    {'urls': 'stun:stun.oriontelekom.rs:3478'},
    {'urls': 'stun:stun.outland-net.de:3478'},
    {'urls': 'stun:stun.ozekiphone.com:3478'},
    {'urls': 'stun:stun.patlive.com:3478'},
    {'urls': 'stun:stun.personal-voip.de:3478'},
    {'urls': 'stun:stun.petcube.com:3478'},
    {'urls': 'stun:stun.phone.com:3478'},
    {'urls': 'stun:stun.phoneserve.com:3478'},
    {'urls': 'stun:stun.pjsip.org:3478'},
    {'urls': 'stun:stun.poivy.com:3478'},
    {'urls': 'stun:stun.powerpbx.org:3478'},
    {'urls': 'stun:stun.powervoip.com:3478'},
    {'urls': 'stun:stun.ppdi.com:3478'},
    {'urls': 'stun:stun.prizee.com:3478'},
    {'urls': 'stun:stun.qq.com:3478'},
    {'urls': 'stun:stun.qvod.com:3478'},
    {'urls': 'stun:stun.rackco.com:3478'},
    {'urls': 'stun:stun.rapidnet.de:3478'},
    {'urls': 'stun:stun.rb-net.com:3478'},
    {'urls': 'stun:stun.refint.net:3478'},
    {'urls': 'stun:stun.remote-learner.net:3478'},
    {'urls': 'stun:stun.rixtelecom.se:3478'},
    {'urls': 'stun:stun.rockenstein.de:3478'},
    {'urls': 'stun:stun.rolmail.net:3478'},
    {'urls': 'stun:stun.rounds.com:3478'},
    {'urls': 'stun:stun.rynga.com:3478'},
    {'urls': 'stun:stun.samsungsmartcam.com:3478'},
    {'urls': 'stun:stun.schlund.de:3478'},
    {'urls': 'stun:stun.services.mozilla.com:3478'},
    {'urls': 'stun:stun.sigmavoip.com:3478'},
    {'urls': 'stun:stun.sip.us:3478'},
    {'urls': 'stun:stun.sipdiscount.com:3478'},
    {'urls': 'stun:stun.sipgate.net:10000'},
    {'urls': 'stun:stun.sipgate.net:3478'},
    {'urls': 'stun:stun.siplogin.de:3478'},
    {'urls': 'stun:stun.sipnet.net:3478'},
    {'urls': 'stun:stun.sipnet.ru:3478'},
    {'urls': 'stun:stun.siportal.it:3478'},
    {'urls': 'stun:stun.sippeer.dk:3478'},
    {'urls': 'stun:stun.siptraffic.com:3478'},
    {'urls': 'stun:stun.skylink.ru:3478'},
    {'urls': 'stun:stun.sma.de:3478'},
    {'urls': 'stun:stun.smartvoip.com:3478'},
    {'urls': 'stun:stun.smsdiscount.com:3478'},
    {'urls': 'stun:stun.snafu.de:3478'},
    {'urls': 'stun:stun.softjoys.com:3478'},
    {'urls': 'stun:stun.solcon.nl:3478'},
    {'urls': 'stun:stun.solnet.ch:3478'},
    {'urls': 'stun:stun.sonetel.com:3478'},
    {'urls': 'stun:stun.sonetel.net:3478'},
    {'urls': 'stun:stun.sovtest.ru:3478'},
    {'urls': 'stun:stun.speedy.com.ar:3478'},
    {'urls': 'stun:stun.spokn.com:3478'},
    {'urls': 'stun:stun.srce.hr:3478'},
    {'urls': 'stun:stun.ssl7.net:3478'},
    {'urls': 'stun:stun.stunprotocol.org:3478'},
    {'urls': 'stun:stun.symform.com:3478'},
    {'urls': 'stun:stun.symplicity.com:3478'},
    {'urls': 'stun:stun.sysadminman.net:3478'},
    {'urls': 'stun:stun.t-online.de:3478'},
    {'urls': 'stun:stun.tagan.ru:3478'},
    {'urls': 'stun:stun.tatneft.ru:3478'},
    {'urls': 'stun:stun.teachercreated.com:3478'},
    {'urls': 'stun:stun.tel.lu:3478'},
    {'urls': 'stun:stun.telbo.com:3478'},
    {'urls': 'stun:stun.telefacil.com:3478'},
    {'urls': 'stun:stun.tis-dialog.ru:3478'},
    {'urls': 'stun:stun.tng.de:3478'},
    {'urls': 'stun:stun.twt.it:3478'},
    {'urls': 'stun:stun.u-blox.com:3478'},
    {'urls': 'stun:stun.ucallweconn.net:3478'},
    {'urls': 'stun:stun.ucsb.edu:3478'},
    {'urls': 'stun:stun.ucw.cz:3478'},
    {'urls': 'stun:stun.uls.co.za:3478'},
    {'urls': 'stun:stun.unseen.is:3478'},
    {'urls': 'stun:stun.usfamily.net:3478'},
    {'urls': 'stun:stun.veoh.com:3478'},
    {'urls': 'stun:stun.vidyo.com:3478'},
    {'urls': 'stun:stun.vipgroup.net:3478'},
    {'urls': 'stun:stun.virtual-call.com:3478'},
    {'urls': 'stun:stun.viva.gr:3478'},
    {'urls': 'stun:stun.vivox.com:3478'},
    {'urls': 'stun:stun.vline.com:3478'},
    {'urls': 'stun:stun.vo.lu:3478'},
    {'urls': 'stun:stun.vodafone.ro:3478'},
    {'urls': 'stun:stun.voicetrading.com:3478'},
    {'urls': 'stun:stun.voip.aebc.com:3478'},
    {'urls': 'stun:stun.voip.blackberry.com:3478'},
    {'urls': 'stun:stun.voip.eutelia.it:3478'},
    {'urls': 'stun:stun.voiparound.com:3478'},
    {'urls': 'stun:stun.voipblast.com:3478'},
    {'urls': 'stun:stun.voipbuster.com:3478'},
    {'urls': 'stun:stun.voipbusterpro.com:3478'},
    {'urls': 'stun:stun.voipcheap.co.uk:3478'},
    {'urls': 'stun:stun.voipcheap.com:3478'},
    {'urls': 'stun:stun.voipfibre.com:3478'},
    {'urls': 'stun:stun.voipgain.com:3478'},
    {'urls': 'stun:stun.voipgate.com:3478'},
    {'urls': 'stun:stun.voipinfocenter.com:3478'},
    {'urls': 'stun:stun.voipplanet.nl:3478'},
    {'urls': 'stun:stun.voippro.com:3478'},
    {'urls': 'stun:stun.voipraider.com:3478'},
    {'urls': 'stun:stun.voipstunt.com:3478'},
    {'urls': 'stun:stun.voipwise.com:3478'},
    {'urls': 'stun:stun.voipzoom.com:3478'},
    {'urls': 'stun:stun.vopium.com:3478'},
    {'urls': 'stun:stun.voxgratia.org:3478'},
    {'urls': 'stun:stun.voxox.com:3478'},
    {'urls': 'stun:stun.voys.nl:3478'},
    {'urls': 'stun:stun.voztele.com:3478'},
    {'urls': 'stun:stun.vyke.com:3478'},
    {'urls': 'stun:stun.webcalldirect.com:3478'},
    {'urls': 'stun:stun.whoi.edu:3478'},
    {'urls': 'stun:stun.wifirst.net:3478'},
    {'urls': 'stun:stun.wwdl.net:3478'},
    {'urls': 'stun:stun.xs4all.nl:3478'},
    {'urls': 'stun:stun.xtratelecom.es:3478'},
    {'urls': 'stun:stun.yesss.at:3478'},
    {'urls': 'stun:stun.zadarma.com:3478'},
    {'urls': 'stun:stun.zadv.com:3478'},
    {'urls': 'stun:stun.zoiper.com:3478'},
    {'urls': 'stun:stun1.faktortel.com.au:3478'},
    {'urls': 'stun:stun1.l.google.com:19302'},
    {'urls': 'stun:stun1.voiceeclipse.net:3478'},
    {'urls': 'stun:stun2.l.google.com:19302'},
    {'urls': 'stun:stun3.l.google.com:19302'},
    {'urls': 'stun:stun4.l.google.com:19302'},
    {'urls': 'stun:stunserver.org:3478'}
]

class BloomFilter:
    def __init__(self, size, hash_count):
        self.size = size
        self.hash_count = hash_count
        self.bit_array = bitarray(size)
        self.bit_array.setall(0)

    def _hashes(self, item):
        return [int(hashlib.sha256((str(item)+str(i)).encode()).hexdigest(),16) % self.size
                for i in range(self.hash_count)]

    def add(self, item):
        for h in self._hashes(item):
            self.bit_array[h] = 1

    def seen(self, item):
        return all(self.bit_array[h] for h in self._hashes(item))


class ScalableBloomFilter:
    def __init__(self, initial_size=958000, initial_hashes=7, growth_factor=2, max_fp=0.01):
        self.filters = [BloomFilter(initial_size, initial_hashes)]
        self.growth_factor = growth_factor
        self.max_fp = max_fp
        self.count = 0  # approximate number of items added

    def add(self, item):
        bf = self.filters[-1]
        bf.add(item)
        self.count += 1
        # check if we should scale
        fp_rate = self._false_positive_estimate(bf)
        if fp_rate > self.max_fp:
            new_size = bf.size * self.growth_factor
            new_hashes = max(1, bf.hash_count)  # you can adjust this
            self.filters.append(BloomFilter(new_size, new_hashes))

    def seen(self, item):
        return any(bf.seen(item) for bf in self.filters)

    def _false_positive_estimate(self, bf):
        # p ≈ (1 - e^(-kn/m))^k
        k = bf.hash_count
        m = bf.size
        n = self.count
        return (1 - math.exp(-k * n / m)) ** k

def channel_label(a, b):
    a = int.from_bytes(bytes.fromhex(a), byteorder='big')
    b = int.from_bytes(bytes.fromhex(b), byteorder='big')

    first = min(a, b)
    second = max(a, b)
    return hashlib.sha3_256(int.to_bytes(first, 32, byteorder='big') + int.to_bytes(second, 32, byteorder='big')).digest()[:20].hex()



def hash_rate():
    start = time.time()
    count = 0
    data = 'hello'.encode()
    while time.time() - start < 1:  # 1 second test
        hashlib.sha3_256(hashlib.sha3_384(data).digest()).digest()
        count += 1
    return count

def message_to_integer(msg):
    h = hashlib.sha3_256(hashlib.sha3_384(json.dumps(msg).encode()).digest()).digest()
    return int.from_bytes(h, byteorder='big'), h

def should_accept_offer(local_id, remote_id):
    # simple lexicographical tie-breaker
    return local_id < remote_id

def hashcash(msg):
    hashcash_start = time.monotonic()
    nonce = 0
    msg['nonce'] = nonce
    value, h = message_to_integer(msg)
    max_value = 2**256 - 1
    minimum = max_value // 65536
    while value > minimum:
        nonce += 1
        msg['nonce'] = nonce
        value, h = message_to_integer(msg)
    # if verbose:
    #     print(f"[*] HASHCASH: {h.hex()} produced in {(time.monotonic() - hashcash_start):0.2f}s")
    return msg, h

def verify_hashcash(msg):
    value, h = message_to_integer(msg)
    max_value = 2**256 - 1
    minimum = max_value // 65536
    return value <= minimum

class UDPProtocol(asyncio.DatagramProtocol):
    def __init__(self, on_message=None, loop=None):
        self.on_message = on_message  # callback for received messages
        self.loop = loop or asyncio.get_event_loop()
        self.transport = None

    def connection_made(self, transport):
        self.transport = transport
        if verbose:
            print("[*] UDP connection ready", flush=True)

    def datagram_received(self, data, addr):
        message = data.decode()
        # print(f"Received {message} from {addr}")
        self.on_message.on_message(message, addr)

    def error_received(self, exc):
        if verbose:
            print(f"Error received: {exc}", flush=True)

    def connection_lost(self, exc):
        if verbose:
            print("[-] UDP connection closed", flush=True)

    # send message method
    def send(self, message, addr):
        if isinstance(message, str):
            message = message.encode()
        if isinstance(addr, str):
            ip, port = addr.split(':')
            addr = (ip, int(port))
        self.transport.sendto(message, addr)

msg_ping = 0x0022FF222
# msg_pong = 0x0022FF444
msg_rely = 0x0022FF666
msg_psub = 0x0022FF999
msg_json = 0x0022FF333

# def ping_msg_():
#     return struct.pack('>I', msg_ping) + struct.pack('>f', time.monotonic())
# def pong_msg_():
#     return struct.pack('>I', msg_pong) + struct.pack('>f', time.monotonic())
def pack_relay(content):
    return struct.pack('>I', msg_rely) + struct.pack('>I', len(content)) + content 
def pack_json(content):
    if isinstance(content, str):
        content = content.encode()
    if isinstance(content, dict):
        content = json.dumps(content).encode()
    return struct.pack('>I', msg_json) + struct.pack('>I', len(content)) + content
def pack_sub(content, destination):
    return struct.pack('>I', msg_psub) + struct.pack('>I', len(content)) + content

class LPC:
    def __init__(self, node):
        self.node = node

    def set_protocol(self, protocol):
        self.protocol = protocol
    
    def on_message(self, msg, _): # received from local client (inbound)
        msg = json.loads(msg)
        msg_type = msg['Type']
        if msg_type == 'Broadcast':
            self.node.lpc_broadcast(msg['Content'])
        elif msg_type in ('Submit', 'Request'):
            self.node.lpc_submit(msg['Content'], msg['Target'])

    def send(self, peer, msg): # incoming; send to client (outbound)
        msg = json.dumps({
            'Sender': peer,
            'Content': msg.hex()
        })
        self.protocol.send(msg, lpc_to)

async def start_lpc(node):
    lpc = LPC(node)
    loop = asyncio.get_running_loop()
    transport, protocol = await loop.create_datagram_endpoint(
        lambda: UDPProtocol(lpc, loop),
        local_addr=lpc_from
    )
    lpc.set_protocol(protocol)
    node.set_lpc(lpc)
    if verbose:
        print('[*] LPC Listening on ', lpc_from, flush=True)

    try:
        while True:
            await asyncio.sleep(0.001)
    except KeyboardInterrupt:
        transport.close()

class RTCNode:
    def __init__(self, node):
        self.channels = {}
        self.peer_connections = {}
        self.node = node  # e.g. your UDP or signaling layer reference
        self.filter = ScalableBloomFilter()
        self.tried = set()

    def send(self, peer, message):
        """
        Send a message to a peer over its data channel.
        """
        if not self.filter.seen(message):
            self.filter.add(message)
        channel = self.channels.get(peer)
        if channel and channel.readyState == "open":
            channel.send(message)
            return True
        else:
            if verbose:
                print(f"[!] Cannot send to {peer}: channel not open", flush=True)
            return False
        
    def broadcast(self, msg, exclude=set()):
        """
        Send a message to all connected peers except those in the 'exclude' set.
        """
        if not self.filter.seen(msg):
            self.filter.add(msg)
        for peer, channel in self.channels.items():
            if peer in exclude:
                continue
            if channel.readyState == "open":
                channel.send(msg)
            # else:
            #     if verbose:
            #         print(f"[!] Skipping {peer}: channel not open", flush=True)

    async def rtc_offer(self, me, peer):
        if self.node.relay_only:
            return
        self.tried.add(peer)
        rtc_ice_servers = [RTCIceServer(**s) for s in stun_servers]
        configuration = RTCConfiguration(iceServers=rtc_ice_servers)
        pc = RTCPeerConnection(configuration)
        self.peer_connections[peer] = pc

        label = channel_label(me, peer)
        channel = pc.createDataChannel(label)
        self.channels[peer] = channel

        bloom_filter = ScalableBloomFilter()
        last_filter_wipe = time.monotonic()

        @channel.on("message")
        def on_message(message):
            nonlocal bloom_filter, last_filter_wipe
            if bloom_filter.seen(message) or self.filter.seen(message):
                return
            now_time = time.monotonic()
            if now_time - last_filter_wipe > 3600:
                bloom_filter = ScalableBloomFilter()
                last_filter_wipe = now_time
            bloom_filter.add(message)
            # if verbose:
            #     print(f"[*] {label} Received {hashlib.sha256(message).hexdigest()} from {peer}", flush=True)
            asyncio.create_task(self.node.receive_message(channel, label, peer, message))
            # self.node.receive_message(channel, label, peer, message)
        @channel.on("open")
        def on_open():
            # if verbose:
            print(f"[*] DataChannel to {peer} is open and ready for messages.", flush=True)
            # asyncio.create_task(self.node.ping_msg(channel))
            self.node.ping()

        # Handle ICE candidates found locally
        @pc.on("icecandidate")
        async def on_icecandidate(event):
            # print('candidates!')
            if event.candidate:
                payload = json.dumps({
                        "candidate": event.candidate.to_sdp(),
                        "sdpMid": event.candidate.sdpMid,
                        "sdpMLineIndex": event.candidate.sdpMLineIndex
                })
                encrypted = self.node.encrypt_message(payload.encode(), peer)
                asyncio.create_task(self.node.broadcast_json({
                    "Type": "RelayCandidate",
                    "Sender": self.node.id,
                    "Recipient": peer,
                    "Candidate": encrypted,
                    'Timestamp': time.monotonic()
                }, retry=5))

        offer = await pc.createOffer()
        await pc.setLocalDescription(offer)
        payload = pc.localDescription.sdp
        encrypted = self.node.encrypt_message(payload.encode(), peer)
        asyncio.create_task(self.node.broadcast_json({
            "Type": "RelayOffer",
            "Sender": self.node.id,
            "Recipient": peer,
            "Offer": encrypted,
            'Timestamp': time.monotonic()
        }, retry=5))
        if verbose:
            print(f"[*] Offer sent to {peer}", flush=True)

        await self.keep_alive(pc, peer)

    async def rtc_answer(self, me, peer, offer_sdp):
        if self.node.relay_only:
            return
        self.tried.add(peer)
        rtc_ice_servers = [RTCIceServer(**s) for s in stun_servers]
        configuration = RTCConfiguration(iceServers=rtc_ice_servers)
        pc = RTCPeerConnection(configuration)
        self.peer_connections[peer] = pc
        label = channel_label(me, peer)
        bloom_filter = ScalableBloomFilter()
        last_filter_wipe = time.monotonic()

        @pc.on("datachannel")
        def on_datachannel(channel):
            # if verbose:
            print(f"[*] Incoming channel from {peer}: {channel.label}", flush=True)
            self.node.ping()
            # asyncio.create_task(self.node.ping_msg(channel))
            self.channels[peer] = channel

            @channel.on("message")
            def on_message(message):
                nonlocal bloom_filter, last_filter_wipe
                if bloom_filter.seen(message) or self.filter.seen(message):
                    return
                now_time = time.monotonic()
                if now_time - last_filter_wipe > 3600:
                    bloom_filter = ScalableBloomFilter()
                    last_filter_wipe = now_time
                bloom_filter.add(message)
                # if verbose:
                #     print(f"[*] {label} Received {hashlib.sha256(message).hexdigest()} from {peer}", flush=True)
                # print(f"[*] {label} Received {message} from {peer}")
                asyncio.create_task(self.node.receive_message(channel, label, peer, message))

        @pc.on("icecandidate")
        async def on_icecandidate(event):
            # print('candidates!')
            if event.candidate:
                payload = json.dumps({
                        "candidate": event.candidate.to_sdp(),
                        "sdpMid": event.candidate.sdpMid,
                        "sdpMLineIndex": event.candidate.sdpMLineIndex
                })
                encrypted = self.node.encrypt_message(payload.encode(), peer)
                asyncio.create_task(self.node.broadcast_json({
                    "Type": "RelayCandidate",
                    "Sender": self.node.id,
                    "Recipient": peer,
                    "Candidate": encrypted,
                    'Timestamp': time.monotonic()
                }, retry=5))

        await pc.setRemoteDescription(RTCSessionDescription(sdp=offer_sdp, type="offer"))
        answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)
        payload = pc.localDescription.sdp
        encrypted = self.node.encrypt_message(payload.encode(), peer)
        asyncio.create_task(self.node.broadcast_json({
            "Type": "RelayAnswer",
            "Sender": self.node.id,
            "Recipient": peer,
            "Answer": encrypted,
            'Timestamp': time.monotonic()
        }, retry=5))

        if verbose:
            print(f"[*] Answer sent to {peer}", flush=True)

        await self.keep_alive(pc, peer)

    async def keep_alive(self, pc, peer, timeout=180):
        done = asyncio.Event()

        @pc.on("connectionstatechange")
        def on_state_change():
            # if verbose:
            print(f"[{peer}] Connection state: {pc.connectionState}", flush=True)
            if pc.connectionState in ("failed", "closed"):
                done.set()
                self.channels.pop(peer, None)
                self.peer_connections.pop(peer, None)

        # async def ping_loop():
        #     while not done.is_set():
        #         channel = self.channels.get(peer)
        #         if channel and channel.readyState == "open":
        #             try:
        #                 await self.node.ping_msg(channel)
        #             except Exception as e:
        #                 print(f"[!] Keep-alive ping failed for {peer}: {e}", flush=True)
        #                 if verbose:
        #                     traceback.print_exc()
                            
        #         await asyncio.sleep(20)

        # Watchdog for timeout
        async def timeout_watchdog():
            await asyncio.sleep(timeout)
            # If still not connected, close
            if pc.connectionState != "connected":
                # if verbose:
                print(f"[!] Connection to {peer} timed out after {timeout} seconds", flush=True)
                if pc.connectionState not in ("closed", "failed"):
                    await pc.close()
                self.channels.pop(peer, None)
                self.peer_connections.pop(peer, None)
                done.set()  # ensure keep_alive ends

        # Run both: normal state change listener and watchdog
        await asyncio.gather(done.wait(), timeout_watchdog())#, ping_loop())

    async def handle_signal(self, me, peer, candidate):
        pc = self.peer_connections.get(peer)
        candidate = RTCIceCandidate(
            sdpMid=candidate["sdpMid"],
            sdpMLineIndex=candidate["sdpMLineIndex"],
            candidate=candidate["candidate"])
        print('[*] Received ICE Candidate from peer ', peer, flush=True)
        await pc.addIceCandidate(candidate)

    async def cancel_connection(self, pc):
        """
        Fully cancel all ongoing WebRTC offers/answers, ICE/STUN retries, and data channels.
        """
        try:
            # Step 1: Roll back negotiation if possible
            if pc.signalingState in ("have-local-offer", "have-remote-offer"):
                try:
                    await pc.setLocalDescription(
                        RTCSessionDescription(sdp="", type="rollback")
                    )
                    if verbose:
                        print("[!] Rolled back negotiation.", flush=True)
                except Exception as e:
                    if verbose:
                        print(f"[!] Rollback failed or not supported: {e}", flush=True)

            # Step 2: Gracefully close peer connection
            if pc.connectionState not in ("closed",):
                await pc.close()
                if verbose:
                    print("[!] Peer connection closed.", flush=True)

            # Step 3: Kill all ICE transports and STUN timers manually
            for trans in getattr(pc, "_transceivers", []):
                ice = getattr(trans, "sender", None)
                if ice and hasattr(ice, "transport") and hasattr(ice.transport, "iceTransport"):
                    ice_transport = ice.transport.iceTransport
                    if ice_transport and hasattr(ice_transport, "_connection"):
                        conn = ice_transport._connection
                        if hasattr(conn, "_transactions"):
                            for t in conn._transactions.values():
                                if not t._future.done():
                                    t._future.cancel()
                            conn._transactions.clear()

            if verbose:
                print("[!] All ICE/STUN transactions cancelled cleanly.", flush=True)

        except Exception as e:
            if verbose:
                print(f"[!] Error while cancelling connection: {e}", flush=True)



"""
    Message Types:
        Announce: Announce the node on the network
        RelayOffer: Send a WebRTC offer (From, To, Offer)
        RelayAnswer: Send a WebRTC answer (From, To, Answer)
        RelayCandidate: Send a WebRTC answer (From, To, Candidate)
"""

class Node:
    def __init__(self, private_key, public_key, id, relay_only):
        self.private_key = private_key
        self.public_key = public_key
        self.id = id
        self.dht = set()
        self.peers = set()
        self.filter = ScalableBloomFilter()
        self.rtcnode = RTCNode(self)
        self.relay_only = relay_only
        self.lpc = None
        self.intents = set()
        self.last_seen = {}

    def set_lpc(self, lpc):
        self.lpc = lpc

    def exchange_secret(self, peer_public):
        return self.private_key.exchange(peer_public)
    
    def derive_key(self, shared_secret):
        # ---- Derive a symmetric key from the shared secret ----
        return HKDF(
            algorithm=hashes.SHA256(),
            length=32,             # 32 bytes = 256 bits
            salt=None,
            info=b'handshake data',
        ).derive(shared_secret)
    
    def encrypt(self, key, msg):
        iv = os.urandom(12)
        encryptor = Cipher(
            algorithms.AES(key),
            modes.GCM(iv)
        ).encryptor()

        ciphertext = encryptor.update(msg) + encryptor.finalize()
        tag = encryptor.tag
        return iv, ciphertext, tag
    
    def decrypt(self, key, iv, ciphertext, tag):
        decryptor = Cipher(
            algorithms.AES(key),
            modes.GCM(iv, tag)
        ).decryptor()

        return decryptor.update(ciphertext) + decryptor.finalize()
    
    def process_intent(self, sender, intent, addr):
        active_channels = len(self.rtcnode.channels)
        if intent == "Offer":
            if should_accept_offer(sender, self.id) and active_channels < 200:
                if verbose:
                    print(f"[*] Creating RTC offer for {sender} from intent", flush=True)
                if self.rtcnode.peer_connections.get(sender):
                    return
                asyncio.create_task(self.rtcnode.rtc_offer(self.id, sender))

    async def process_offer(self, sender, offer, addr):
        """Called when we receive an SDP offer."""
        if verbose:
            print(f"[*] Processing offer from {sender}", flush=True)
        pc = self.rtcnode.peer_connections.get(sender)
        if pc:
            if pc.signalingState == "have-local-offer": # already sent an offer
                if should_accept_offer(self.id, sender):
                    await self.rtcnode.cancel_connection(pc)
                    pc.close()
                    self.rtcnode.peer_connections.pop(sender, None)
                    self.rtcnode.channels.pop(sender, None)
                    await self.rtcnode.rtc_answer(self.id, sender, offer)
                else:
                    if verbose:
                        print('[!] Rejected offer from ', sender, flush=True)
                    return
            elif pc.signalingState == "stable":
                await self.rtcnode.rtc_answer(self.id, sender, offer)
        else:
            await self.rtcnode.rtc_answer(self.id, sender, offer)

    def process_answer(self, sender, answer, addr):
        """Called when we receive an SDP answer."""
        pc = self.rtcnode.peer_connections.get(sender)
        if pc:
            desc = RTCSessionDescription(sdp=answer, type="answer")
            asyncio.create_task(pc.setRemoteDescription(desc))

    def process_candidate(self, sender, candidate, addr):
        """Called when we receive an ICE candidate."""
        candidate_data = json.loads(candidate)
        pc = self.rtcnode.peer_connections.get(sender)
        if pc:
            asyncio.create_task(self.rtcnode.handle_signal(self.id, sender, candidate_data))

    def encrypt_message(self, msg, recipient):
        recipient_bytes     = bytes.fromhex(recipient)
        recipient_pubkey    = x25519.X25519PublicKey.from_public_bytes(recipient_bytes)
        secret  = self.exchange_secret(recipient_pubkey)
        key     = self.derive_key(secret)
        iv, msg, tag = self.encrypt(key, msg)
        return {
            'Secret': msg.hex(),
            'IV': iv.hex(),
            'Tag': tag.hex()
        }
    
    def decrypt_message(self, sender, msg):
        sender_bytes    = bytes.fromhex(sender)
        sender_pubkey   = x25519.X25519PublicKey.from_public_bytes(sender_bytes)
        secret  = self.exchange_secret(sender_pubkey)
        key     = self.derive_key(secret)
        secret  = bytes.fromhex(msg['Secret'])
        iv      = bytes.fromhex(msg['IV'])
        tag     = bytes.fromhex(msg['Tag'])
        return self.decrypt(key, iv, secret, tag)
    
    def receive_intent(self, sender, msg, addr):
        if self.relay_only:
            return
        self.process_intent(sender, self.decrypt_message(sender, msg).decode('utf-8'), addr)
    def receive_offer(self, sender, msg, addr):
        if self.relay_only:
            return
        asyncio.create_task(self.process_offer(sender, self.decrypt_message(sender, msg).decode('utf-8'), addr))
    def receive_answer(self, sender, msg, addr):
        if self.relay_only:
            return
        self.process_answer(sender, self.decrypt_message(sender, msg).decode('utf-8'), addr)
    def receive_candidate(self, sender, msg, addr):
        if self.relay_only:
            return
        self.process_candidate(sender, self.decrypt_message(sender, msg).decode('utf-8'), addr)
    
    async def maintain_rtc_channels(self, max_channels=200):
        """
        Ensure that there are at most max_channels active data channels.
        Creates offers to peers that don't yet have channels.
        """
        if self.relay_only:
            return
        active_channels = len(self.rtcnode.channels)
        if active_channels >= max_channels:
            return  # already at limit
        
        # Peers without an active channel
        peers_to_connect = [hd for hd in self.dht if hd not in self.rtcnode.channels]

        for hd in peers_to_connect:
            if hd == self.id:
                continue

            if len(self.rtcnode.channels) >= max_channels:
                break  # stop once we hit max

            if hd in self.rtcnode.tried:
                continue

            if should_accept_offer(self.id, hd) and hd not in self.intents:
                if verbose:
                    print(f"[*] Creating RTC intent for {hd}", flush=True)
                self.intents.add(hd)
                asyncio.create_task(self.broadcast_intent(hd, 'Offer'))
            elif hd not in self.intents:
                self.intents.add(hd)
                if verbose:
                    print(f"[*] Creating RTC offer for {hd}", flush=True)
                asyncio.create_task(self.rtcnode.rtc_offer(self.id, hd))

    async def broadcast_intent(self, recipient, intent, retry=5):
        msg, _ = hashcash({
            'Type': 'RelayIntent',
            'Sender': self.id,
            'Recipient': recipient,
            'Intent': self.encrypt_message(intent.encode(), recipient),
            'Timestamp': time.monotonic()
        })
        self.broadcast(json.dumps(msg), exclude=set(), retry=retry)

    async def broadcast_json(self, msg, exclude=set(), excluded_nodes=set(), retry=5):
        msg, _ = hashcash(msg)
        self.broadcast(json.dumps(msg), exclude, excluded_nodes, retry=retry)

    def prune_inactive_peers(self, timeout=120):
        now = time.monotonic()
        to_remove = [peer for peer, last in self.last_seen.items() if now - last > timeout]

        for peer in to_remove:
            if peer in self.peers:
                self.peers.remove(peer)
            self.last_seen.pop(peer, None)
            if verbose:
                print(f"[-] Removed inactive peer {peer}", flush=True)

    
    def on_message(self, msg, addr=None, node=None):
        if self.filter.seen(msg):
            return
        self.filter.add(msg)
        original_message = str(msg)
        if addr:
            self.peers.add(addr)
            self.last_seen[addr] = time.monotonic()
        msg = json.loads(msg)
        if not verify_hashcash(msg):
            return
        if msg['Type'] == 'Ping':
            sample = msg['Sample']
            if verbose:
                print('[+] Received Sample: ', sample)
            for i in sample:
                if i == self.id:
                    continue
                self.dht.add(i)
            if verbose:
                print(f"[*] Received (PING) {sample}")
            return
        elif msg['Type'] == 'Announce':
            dht_key = msg['Content']['id']
            ip      = msg['Content']['ip']
            port    = msg['Content']['port']
            if verbose:
                print(f"[*] Received (ANNOUNCE) {dht_key}")
            self.dht.add(dht_key)
            asyncio.create_task(self.connect_udp((ip, int(port))))
        elif msg['Type'] == 'RelayIntent':
            sender = msg['Sender']
            recipient = msg['Recipient']
            intent = msg['Intent']
            if verbose:
                print(f"[*] Received (INTENT) {sender}:{recipient}")
            if recipient == self.id:
                self.receive_intent(sender, intent, addr)
                return
        elif msg['Type'] == 'RelayOffer':
            sender = msg['Sender']
            recipient = msg['Recipient']
            offer = msg['Offer']
            if verbose:
                print(f"[*] Received (OFFER) {sender}:{recipient}")
            if recipient == self.id:
                self.receive_offer(sender, offer, addr)
                return
        elif msg['Type'] == 'RelayAnswer':
            sender = msg['Sender']
            recipient = msg['Recipient']
            answer = msg['Answer']
            if verbose:
                print(f"[*] Received (ANSWER) {sender}:{recipient}")
            if recipient == self.id:
                self.receive_answer(sender, answer, addr)
                return
        elif msg['Type'] == 'RelayCandidate':
            sender = msg['Sender']
            recipient = msg['Recipient']
            candidate = msg['Candidate']
            if verbose:
                print(f"[*] Received (CANDIDATE) {sender}:{recipient}")
            if recipient == self.id:
                self.receive_candidate(sender, candidate, addr)
                return
        self.broadcast(original_message, set([addr]), set([node]), retry=2)
    
    async def connect_udp(self, addr):
        if addr in self.peers:
            return
        self.protocol.send(self.announce, addr)
        self.peers.add(addr)
        self.last_seen[addr] = time.monotonic()

    async def bootstrap(self, protocol, addresses):
        self.protocol = protocol
        announcement, h = hashcash({'Type': 'Announce', 'Content': {'id': self.id, 'ip':public_ip, 'port': listen_on[1]}, 'Timestamp': time.monotonic()})
        self.announce = json.dumps(announcement)
        for addr in addresses:
            await self.connect_udp(addr)

    def announce_self(self):
        announcement, h = hashcash({'Type': 'Announce', 'Content': {'id': self.id, 'ip':public_ip, 'port': listen_on[1]}, 'Timestamp': time.monotonic()})
        self.announce = json.dumps(announcement)
        self.broadcast(self.announce, set())

    def clear_filters(self):
        self.rtcnode.filter = ScalableBloomFilter()
        self.filter = ScalableBloomFilter()

    def ping(self):
        dht = list(self.dht)
        sample = random.sample(dht, min(4, len(dht)))
        ping, _ = hashcash({
            'Type': 'Ping',
            'Sample': sample,
            'Timestamp': time.monotonic()
        })
        self.broadcast(json.dumps(ping), exclude=set())

    def broadcast(self, msg, exclude, excluded_nodes=set(), retry=2):
        if not self.filter.seen(msg):
            self.filter.add(msg)
        # print('[*] Sending ', msg)
        self.rtcnode.broadcast(pack_json(msg), exclude=excluded_nodes)

        for _ in range(retry):
            for peer in self.peers:
                if peer in exclude:
                    continue
                self.protocol.send(msg, peer)

    async def receive_message(self, channel, label, peer, msg):
        try:
            # print('msg')
            # First 4 bytes = msg_rely
            msg_type = struct.unpack('>I', msg[:4])[0]
            if msg_type == msg_ping:
                if verbose:
                    print(f"[*] Received Ping from {peer}")
                # asyncio.create_task(self.pong_msg(channel))
            # elif msg_pong:
            #     if verbose:
            #         print("[*] Received Pong from {peer}")
            #     return
            elif msg_rely:
                if verbose:
                    print(f"[*] Received RELAY from {peer}")
                # Next 4 bytes = content length
                content_len = struct.unpack('>I', msg[4:8])[0]
                # Remaining bytes = content
                self.lpc.send(peer, msg[8:8 + content_len])
                # self.rtcnode.broadcast(og, set([peer]))
            elif msg_json:
                content_len = struct.unpack('>I', msg[4:8])[0]
                content = msg[8:8 + content_len].decode('utf-8')
                if verbose:
                    print('[*] Received JSON from {peer} {content}')
                self.on_message(content, None, node=peer)
        except:
            if verbose:
                print('[!] A corrupt message received', flush=True)
            traceback.print_exc()
    
    # async def ping_msg(self, channel):
    #     try:
    #         channel.send(ping_msg_())
    #     except:
    #         print('[!] Could not send PING', flush=True)
    # async def pong_msg(self, channel):
    #     try:
    #         channel.send(pong_msg_())
    #     except:
    #         print('[!] Could not send PONG', flush=True)

    async def lpc_broadcast(self, content, exclude=set()):
        try:
            self.rtcnode.broadcast(pack_relay(content), exclude=exclude)
        except:
            print('[!] Could not broadcast MESSAGE', flush=True)

    async def lpc_submit(self, content, peer):
        channel = self.rtcnode.channels.get(peer)
        if channel:
            try:
                channel.send(pack_sub(content))
            except:
                print('[!] Could not send MESSAGE', flush=True)

    async def json_submit(self, content, peer):
        channel = self.rtcnode.channels.get(peer)
        if channel:
            try:
                channel.send(msg_json(content))
            except:
                print('[!] Could not send MESSAGE', flush=True)

    async def json_broadcast(self, content, exclude=set()):
        try:
            self.rtcnode.broadcast(msg_json(content), exclude=exclude)
        except:
            print('[!] Could not broadcast MESSAGE', flush=True)

def filter_bootstrap(addresses):
    my_ip = (public_ip, listen_on[1])
    filtered_addresses = []
    for addr in addresses:
        if addr == my_ip:
            print(f"[!] Removing own address: {addr}", flush=True)
        else:
            print(f"[+] Keeping address: {addr}", flush=True)
            filtered_addresses.append(addr)
    return filtered_addresses
        
async def main():
    global verbose
    argv = sys.argv[1:]
    relay_only = False

    for arg in argv:
        if arg == 'relay_only':
            relay_only = True
            print('[*] Starting in RELAY_ONLY mode', flush=True)
        elif arg == 'verbose':
            verbose = True
            print('[*] Starting in VERBOSE mode', flush=True)
    
    cwd = os.getcwd()
    directory = os.path.join(cwd, 'rpc')
    os.makedirs(directory, exist_ok=True)

    # key path
    key_path = os.path.join(directory, 'keypair.json')

    # load or generate key
    if os.path.exists(key_path):
        with open(key_path, 'r') as f:
            data = json.load(f)
            priv_bytes = bytes.fromhex(data['private_key'])
            pub_bytes = bytes.fromhex(data['public_key'])
                
        private_key = x25519.X25519PrivateKey.from_private_bytes(priv_bytes)
        public_key = x25519.X25519PublicKey.from_public_bytes(pub_bytes)
        # if verbose:
        print("[+] Loaded existing node key:", pub_bytes.hex(), flush=True)
    else:
        private_key = x25519.X25519PrivateKey.generate()
        public_key = private_key.public_key()
        # Serialize to bytes (Base64-encoded or hex)
        priv_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.Raw,
            format=serialization.PrivateFormat.Raw,
            encryption_algorithm=serialization.NoEncryption()
        )
        pub_bytes = public_key.public_bytes(
            encoding=serialization.Encoding.Raw,
            format=serialization.PublicFormat.Raw
        )

        with open(key_path, 'w') as f:
            json.dump({'private_key': priv_bytes.hex(), 'public_key': pub_bytes.hex()}, f)
        # if verbose:
        print("[+] Generated new node key:", pub_bytes.hex(), flush=True)

    
    # create node
    node = Node(private_key, public_key, pub_bytes.hex(), relay_only)
    # if verbose:
    print("[*] Node initialized with ID:", node.id, flush=True)

    bootstrap = filter_bootstrap(bootstrap_nodes)

    loop = asyncio.get_running_loop()
    transport, protocol = await loop.create_datagram_endpoint(
        lambda: UDPProtocol(node, loop),
        local_addr=listen_on
    )
    # if verbose:
    print('[*] Listening on ', listen_on, flush=True)
    await node.bootstrap(protocol, bootstrap)

    if not relay_only:
        asyncio.create_task(start_lpc(node))

    start = time.monotonic()
    last_filter_wipe = time.monotonic()
    last_rtc_wipe = time.monotonic()
    last_announce = time.monotonic()

    node.ping()
    next_ping_interval = random.uniform(15, 25)

    try:
        while True:
            check = time.monotonic()
            if check - start > next_ping_interval:
                print(f"[*] '{len(node.peers)}' Peers, '{len(node.rtcnode.channels)}' Nodes")
                node.ping()
                start = check
                next_ping_interval = random.uniform(15, 25)
                if not relay_only:
                    await node.maintain_rtc_channels(max_channels=128)
            if check - last_announce > 120:
                node.announce_self()
                node.prune_inactive_peers(timeout=120)
                last_announce = check
            if check - last_filter_wipe > 3600:
                node.clear_filters()
                last_filter_wipe = check
            if check - last_rtc_wipe > 60:
                node.rtcnode.tried = set()
                for hd, _ in node.rtcnode.channels.items():
                    node.rtcnode.tried.add(hd)
                last_rtc_wipe = check
            await asyncio.sleep(0.001)
    except KeyboardInterrupt:
        transport.close()

asyncio.run(main())