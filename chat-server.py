import socket
import threading

# Λίστα για την αποθήκευση των συνδεδεμένων clients
clients = []

def broadcast(message, _current_client):
    """Στέλνει το μήνυμα σε όλους ΕΚΤΟΣ από αυτόν που το έστειλε"""
    for client in clients:
        if client != _current_client:
            try:
                client.send(message)
            except:
                # Αν η αποστολή αποτύχει, αφαιρούμε τον client
                clients.remove(client)

def handle_client(conn, addr):
    print(f"[ΣΥΝΔΕΣΗ] {addr} εισήλθε στο chat.")
    
    while True:
        try:
            # Λήψη μηνύματος
            msg = conn.recv(1024)
            if not msg:
                break
            
            # Εκπομπή σε όλους τους άλλους
            broadcast(msg, conn)
            
        except:
            break

    # Καθαρισμός κατά την αποσύνδεση
    print(f"[ΑΠΟΣΥΝΔΕΣΗ] {addr} αποχώρησε.")
    clients.remove(conn)
    conn.close()

def start_chat():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('127.0.0.1', 65432))
    server.listen()
    print("[CHAT SERVER] Σε λειτουργία...")

    while True:
        conn, addr = server.accept()
        clients.append(conn) # Προσθήκη στη λίστα
        thread = threading.Thread(target=handle_client, args=(conn, addr))
        thread.start()

if __name__ == "__main__":
    start_chat()
