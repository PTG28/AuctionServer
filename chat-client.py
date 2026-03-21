import socket
import threading

def receive_messages(client_socket):
    """Συνάρτηση που τρέχει σε ξεχωριστό thread για λήψη μηνυμάτων"""
    while True:
        try:
            message = client_socket.recv(1024).decode('utf-8')
            if message:
                print(f"\n{message}")
            else:
                break
        except:
            print("Αποσυνδέθηκες από τον server.")
            break

def start_client():
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect(('127.0.0.1', 65432))
    
    username = input("Δώσε το όνομά σου: ")

    # Thread για τη λήψη μηνυμάτων (για να μην κολλάει το input)
    thread = threading.Thread(target=receive_messages, args=(client,))
    thread.daemon = True # Κλείνει αυτόματα όταν κλείσει το πρόγραμμα
    thread.start()

    while True:
        msg = input("")
        if msg.lower() == 'quit':
            break
        full_msg = f"{username}: {msg}"
        client.send(full_msg.encode('utf-8'))

    client.close()

if __name__ == "__main__":
    start_client()
