#!/usr/bin/env python3
"""Real REST/STOMP smoke check. Requires Python websockets. Run against an isolated server."""
import asyncio
import json
import os
import urllib.request
import urllib.error
import uuid
import websockets

BASE = os.environ.get('CHAT_TEST_URL', 'http://localhost:7171').rstrip('/')
ORIGIN = os.environ.get('CHAT_TEST_ORIGIN', 'http://127.0.0.1:7170')

def http(path, token=None, method='GET'):
    headers = {'Authorization': 'Bearer ' + token} if token else {}
    with urllib.request.urlopen(urllib.request.Request(BASE + path, headers=headers, method=method)) as response:
        body = response.read()
        return json.loads(body) if body else None

async def send(ws, command, headers=None, body=''):
    await ws.send(command + '\n' + ''.join(f'{k}:{v}\n' for k, v in (headers or {}).items()) + '\n' + body + '\0')

async def receive(ws, wanted):
    while True:
        frame = await asyncio.wait_for(ws.recv(), 8)
        if frame.startswith(wanted):
            return frame
        if frame.startswith('ERROR'):
            raise AssertionError('STOMP rejected the command')

async def connect(token=None):
    ws = await websockets.connect(BASE.replace('http', 'ws', 1) + '/ws-chatapp/websocket', origin=ORIGIN)
    headers = {'accept-version': '1.2', 'host': 'localhost'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    await send(ws, 'CONNECT', headers)
    return ws

async def main():
    suffix = uuid.uuid4().hex[:7]
    alice_name, bob_name = 'a.' + suffix, 'b.' + suffix
    alice = http('/api/v1/auth?username=' + alice_name, method='POST')
    bob = http('/api/v1/auth?username=' + bob_name, method='POST')
    try:
        http('/api/v1/dm/' + bob_name + '/messages?requester=' + alice_name)
        raise AssertionError('Unauthenticated history was accepted')
    except urllib.error.HTTPError as error:
        assert error.code == 401
    unauth = await connect()
    try:
        await receive(unauth, 'CONNECTED')
        raise AssertionError('Unauthenticated STOMP connected')
    except (AssertionError, websockets.exceptions.ConnectionClosed) as error:
        if str(error) == 'Unauthenticated STOMP connected':
            raise
    finally:
        await unauth.close()
    sender = await connect(alice['token'])
    receiver = await connect(bob['token'])
    try:
        await receive(sender, 'CONNECTED')
        await receive(receiver, 'CONNECTED')
        await send(sender, 'SUBSCRIBE', {'id': 'accepted', 'destination': '/user/queue/accepted'})
        await send(receiver, 'SUBSCRIBE', {'id': 'direct', 'destination': '/user/queue/direct'})
        await send(sender, 'SUBSCRIBE', {'id': 'receipts', 'destination': '/user/queue/receipts'})
        command = {'clientMessageId': str(uuid.uuid4()), 'recipientId': bob_name, 'body': 'Retry-safe text', 'type': 'CHAT', 'format': 'TEXT'}
        await send(sender, 'SEND', {'destination': '/ws/chat.send-direct-message', 'content-type': 'application/json'}, json.dumps(command))
        received = json.loads((await receive(receiver, 'MESSAGE')).split('\n\n', 1)[1].rstrip('\0'))
        accepted = json.loads((await receive(sender, 'MESSAGE')).split('\n\n', 1)[1].rstrip('\0'))
        assert received['id'] == accepted['id'] and accepted['clientMessageId'] == command['clientMessageId']
        await send(sender, 'SEND', {'destination': '/ws/chat.send-direct-message', 'content-type': 'application/json'}, json.dumps(command))
        replay = json.loads((await receive(sender, 'MESSAGE')).split('\n\n', 1)[1].rstrip('\0'))
        assert replay['id'] == accepted['id']
        await send(receiver, 'SEND', {'destination': '/ws/chat.delivered-message', 'content-type': 'application/json'}, json.dumps({'messageId': received['id']}))
        receipt = json.loads((await receive(sender, 'MESSAGE')).split('\n\n', 1)[1].rstrip('\0'))
        assert receipt['type'] == 'MESSAGE_DELIVERED' and 'body' not in receipt
        await send(receiver, 'SEND', {'destination': '/ws/chat.read-message', 'content-type': 'application/json'}, json.dumps({'messageId': received['id']}))
        read = json.loads((await receive(sender, 'MESSAGE')).split('\n\n', 1)[1].rstrip('\0'))
        assert read['type'] == 'MESSAGE_READ' and read['readAt'] and read['deliveredAt'] == receipt['deliveredAt']
        history = http('/api/v1/dm/' + bob_name + '/messages', alice['token'])
        assert len(history['messages']) == 1 and history['messages'][0]['readBy'][bob_name]
        # Reconnect with the same credential, then retry the same command after losing the previous socket.
        await sender.close()
        sender = await connect(alice['token'])
        await receive(sender, 'CONNECTED')
        await send(sender, 'SUBSCRIBE', {'id': 'accepted-again', 'destination': '/user/queue/accepted'})
        await send(sender, 'SEND', {'destination': '/ws/chat.send-direct-message', 'content-type': 'application/json'}, json.dumps(command))
        replay = json.loads((await receive(sender, 'MESSAGE')).split('\n\n', 1)[1].rstrip('\0'))
        assert replay['id'] == accepted['id']
        assert len(http('/api/v1/dm/' + bob_name + '/messages', alice['token'])['messages']) == 1
        await send(sender, 'SEND', {'destination': '/topic/chat/activity'}, '{}')
        try:
            await receive(sender, 'CONNECTED')
            raise AssertionError('Direct broker publish was accepted')
        except (AssertionError, websockets.exceptions.ConnectionClosed) as error:
            if str(error) == 'Direct broker publish was accepted':
                raise
        blocked = await connect(alice['token'])
        await receive(blocked, 'CONNECTED')
        await send(blocked, 'SUBSCRIBE', {'id': 'forbidden', 'destination': '/queue/direct'})
        try:
            await receive(blocked, 'CONNECTED')
            raise AssertionError('Raw user queue subscription was accepted')
        except (AssertionError, websockets.exceptions.ConnectionClosed) as error:
            if str(error) == 'Raw user queue subscription was accepted':
                raise
        finally:
            await blocked.close()
        http('/api/v1/logout', bob['token'], method='POST')
        await asyncio.wait_for(receiver.wait_closed(), 4)
        try:
            http('/api/v1/session', bob['token'])
            raise AssertionError('Revoked credential was accepted')
        except urllib.error.HTTPError as error:
            assert error.code == 401
        print('PASS: REST/STOMP credentials, canonical acceptance, duplicate suppression, separate receipts, reconnect and destination restrictions')
    finally:
        await sender.close()
        await receiver.close()

asyncio.run(main())
