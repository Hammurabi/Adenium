# import requests to make our lives easier
import requests
import json
from types import SimpleNamespace

class rpc_noconnection:
    def __init__(self, reason):
        self.response = 'failed'
        self.reason   = reason
# define a 'connection' class to hold all the connection information
class rpc_connection:
    def __init__(self):
        self.ip      = 'localhost'
        self.port    = '12560'
        self.token   = ''
        self.scheme  = 'http'
    def send_request(self, request, arguments):
        # url = self.scheme + "://" + self.ip + ":" + self.port + "/api/get?" + self.package_query(request, arguments)
        # try:
        #     return self.to_json(requests.get(url, allow_redirects=True))
        # except:
        #     return rpc_noconnection('failed to send request to server.'), ''
        arguments['request'] = request
        return self.post_request(arguments)
    def post_request(self, arguments, data_type='text'):
        url = self.scheme + "://" + self.ip + ":" + self.port + "/api?dtype=" + data_type
        try:
            return self.to_json(requests.post(url, arguments))
        except:
            return rpc_noconnection('failed to send request to server.'), ''
    def pipe_request(self, request, arguments, data, data_type='bin'):
        url = self.scheme + "://" + self.ip + ":" + self.port + "/api?dtype=" + data_type + "&" + package_query(request, arguments)
        try:
            return self.to_json(requests.post(url, data=arguments, headers={'Content-Type': 'application/octet-stream'}))
        except:
            return rpc_noconnection('failed to send request to server.'), ''
    def to_json(self, response):
        obj     = json.loads(response.text, object_hook=lambda d: SimpleNamespace(**d))
        dump    = ''
        if hasattr(obj, 'content'):
            dump= json.dumps(json.loads(response.text)['content'], indent=4)
        return obj, dump
    def package_query(self, request, arguments):
        query = 'request=' + request
        for key in arguments:
            query = query + '&' + str(key) + '=' + str(arguments[key])
        return query