# from dataclasses import dataclass
# from typing import Dict
# from pydantic import BaseModel
# from typing import TypedDict
#
#
# @dataclass
# class Data:
#     data: Dict[str, int]
#
#
# print(Data(data={"key2": 42, "key": '42'}).data)
# print(Data(data={"key": '42'}).data)
#
#
# class Data3(TypedDict):
#     data: Dict[str, int]
#
#
# print(Data3(data={"key2": 42, "key": '42'}))
# print(Data3(data={"key": '42'}))
#
#
#


import urllib.request
import json


def send_to_ai(message):
    try:

        url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=AIzaSyC-51kDU90el5xvJATSAo6J1ISqQWHql_E'

        data = {
            "contents": [
                {
                    "parts": [
                        {
                            "text": message
                        }
                    ]
                }
            ]
        }

        # 将字典编码为 JSON 字符串
        json_data = json.dumps(data).encode('utf-8')

        # 创建请求对象
        req = urllib.request.Request(url, data=json_data, headers={'Content-Type': 'application/json'}, method='POST')

        with urllib.request.urlopen(req) as response:
            response_data = json.loads(response.read().decode('utf-8'))
            # print(response_data)
            rsp = response_data['candidates'][0]['content']['parts'][0]['text']
            return rsp
    except Exception as e:
        return f"AI服务异常：{e}"


from flask import Flask, request, jsonify


app = Flask(__name__)


# 用户名和密码
valid_usernames = {
    'admin': 'admin',
    '1': '1',
    '': ''
}


@app.route('/login', methods=['GET'])
def login():
    # 获取请求参数
    username = request.args.get('username')
    password = request.args.get('password')

    # 验证用户名和密码
    if username in valid_usernames and valid_usernames[username] == password:
        return 'success'
    else:
        return 'fail'


@app.route('/register', methods=['POST'])
def register():
    return 'success'


@app.route('/send_message', methods=['POST'])
def send_message():
    data = request.get_json()
    message = data.get('message')
    # 模拟一个服务器返回的消息
    server_message = send_to_ai(message)
    print(server_message)
    return jsonify({"msg": server_message}), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=11771)








