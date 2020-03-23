using System;
using HybridWebSocket;
using model;
using Piisu.CBOR;
using UnityEngine;
using UnityEngine.UI;

public class WebSocketController : MonoBehaviour
{

    private WebSocket webSocket;
    
    // Start is called before the first frame update
    void Start()
    {
        Debug.Log("Start");
        webSocket = WebSocketFactory.CreateInstance("ws://localhost:8081/chat");

        webSocket.OnMessage += data =>
        {
            var message = MessageConverter.Instance.FromBytes(data);
            Debug.Log(message);
        };
        
        webSocket.Connect();
    }

    private void OnDestroy()
    {
        webSocket.Close();
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}
