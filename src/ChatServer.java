import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatServer {
    public static final int PORT = 10666;
    private ServerSocket serverSocket;
    private ArrayList<ChatServerClientThread> clientThreads;

    public ChatServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            clientThreads = new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Fehler beim Öffnen von Port " + PORT);
            System.exit(1);
        }
    }

    public void run() {
        System.out.println("ChatServer gestartet auf Port " + PORT);

        while (true) {
            try {
                Socket neueClientSocket = serverSocket.accept();
                var neuerClient = new ChatServerClientThread(this,
                        neueClientSocket);
                clientThreads.add(neuerClient);
                neuerClient.start();
                System.out.println("Client Nr. " + clientThreads.size() + " " +
                        "verbunden!");
            } catch (IOException e) {
                System.err.println("Fehler bei Verbindung mit Client");
            }
        }
    }

    public void sendeAnAlle(ChatServerClientThread client, String botschaft) {
        for (var empfaenger: clientThreads) {
            empfaenger.sende(client.getName() + ": " + botschaft);
        }
    }

    public void sendeAnAndere(ChatServerClientThread client, String botschaft) {
        for (var empfaenger: clientThreads) {
            if (empfaenger != client) {
                empfaenger.sende(botschaft);
            }
        }
    }

    public void sendeDM(ChatServerClientThread client, String empfaengerName, String botschaft) {
        for (var empfaenger : clientThreads) {
            if (empfaenger.getName().equals(empfaengerName)) {
                empfaenger.sende("Private Nachricht von " + client.getName() + ": " + botschaft);
                return;
            }
        }
        client.sende("Der Empfänger " + empfaengerName + " konnte nicht gefunden werden.");
    }

    public static void main(String[] args) {
        new ChatServer().run();
    }
}

class ChatServerClientThread extends Thread {
    private ChatServer server;
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String ad;
    private String nameS;
    private String nameC;
    private String prevName;
    private int dif = 1;

    public ChatServerClientThread(ChatServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;

        try {
            ad = clientSocket.getInetAddress().getCanonicalHostName();
            nameC = getName();
            nameS = nameC + " (" + ad + ")";
            reader =
                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer =
                    new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben auf dem Client " + nameS);
            System.exit(2);
        }
    }

    public void sende(String botschaft) {
        writer.println(botschaft);
        writer.flush();
    }

    public void listeBefehle() {
        sende("/name {Ihr Name} - Ändert Ihren Namen");
        sende("/quiz - Sie bekommen vom Server eine Rechenaufgabe gestellt");
        sende("/privat {Name des Adressats} {\"Ihre Nachricht\"} - Sie schreiben jemandem " +
                "eine   Privatnachricht");
    }

    public void updateName(String eingabe) {
        prevName = nameC;
        nameC = eingabe.substring(6);
        setName(nameC);
        System.out.println(nameS + " hat seinen Namen geändert zu " + nameC);
        nameS = nameC + " (" + ad + ")";
        server.sendeAnAndere(this, prevName + " hat seinen Namen geändert zu " + nameC);
        sende("Dein Name wurde auf " + nameC + " geändert.");
    }


    public void stelleRechenaufgabe() {
        int x = (int) (Math.random() * Math.pow(10, dif)) + 1;
        int y = (int) (Math.random() * Math.pow(10, dif)) + 1;
        int opID = (int) (Math.random() * 3);
        String[] ops = {"+", "-", "*"};
        String frage = "Wie viel ist " + x + ops[opID] + y + "?";
        int ergebnis;
        if (opID == 0) {
            ergebnis = x + y;
        } else if (opID == 1) {
            ergebnis = x - y;
        } else {
            ergebnis = x * y;
        }
        int versuche = 0;

        sende(frage);

        while (true) {
            try {
                String antwort = reader.readLine();
                versuche++;
                if (antwort != null) {
                    try {
                        int antwortInt = Integer.parseInt(antwort);
                        if (antwortInt == ergebnis) {
                            sende("Richtig! (Anzahl der benötigten Versuche: " + versuche + ")");
                            break;
                        } else {
                            sende("Falsch! Versuche es nochmal.");
                        }
                    } catch (NumberFormatException e) {
                        sende("Ungültige Eingabe! Versuche es nochmal.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Fehler beim Lesen der Antwort");
                break;
            }
        }

        schwierigkeitsgrad(versuche);
    }

    public void schwierigkeitsgrad(int versuche) {
        if (versuche == 1) {
            sende("Die Aufgabe war ja sehr einfach für dich! Willst du den Schwierigkeitsgrad erhö-hen?");
            while (true) {

                try {
                    String antwort = reader.readLine();

                    if (antwort != null) {
                            if (antwort.equalsIgnoreCase("Ja")) {
                                dif++;
                                sende("Der Schwierigkeitsgrad wurde auf " + dif + " erhöht.");
                                System.out.println(nameS + " hat seinen Schwierigkeitsgrad auf " + dif + " erhöht.");
                                break;
                            } else if (antwort.equalsIgnoreCase("Nein")) {
                                sende("Okay, du kannst immer später erhöhen, wenn du willst.");
                                break;
                            } else {
                                sende("Antworte bitte mit Ja oder Nein");
                            }
                    }


                } catch (IOException e) {
                    System.err.println("Fehler beim Lesen der Antwort");
                    break;
                }

            }
        } else if (versuche > 3 && dif > 1) {
            sende("Du hattest scheinbar Probleme mit der Aufgabe! Willst du den Schwierigkeitsgrad senken?");
            while (true) {
                try {
                    String antwort = reader.readLine();

                    if (antwort != null) {
                        if (antwort.equalsIgnoreCase("Ja")) {
                            dif--;
                            sende("Der Schwierigkeitsgrad wurde auf " + dif + " gesenkt.");
                            System.out.println(nameS + " hat seinen Schwierigkeitsgrad auf " + dif + " gesenkt.");
                            break;
                        } else if (antwort.equalsIgnoreCase("Nein")) {
                            break;
                        } else {
                            sende("Antworte bitte mit Ja oder Nein");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Fehler beim Lesen der Antwort");
                    break;
                }

            }
        }
    }

    public void privatNachricht(String eingabe) {
        String[] teile = eingabe.split(" \"");
        server.sendeDM(this, teile[0], teile[1].substring(0,teile[1].length()-1));
    }

    @Override
    public void run() {
        String empfangen;
        System.out.println("ClientThread gestartet mit: " + nameS);
        sende("Willkommen beim Mathe-Server! Gib /quit ein um zu " +
                "beenden.");
        sende("Wenn du eine Liste der Befehle haben willst, " +
                "schreibe /list .");

        while (true) {
            try {
                empfangen = reader.readLine();
                System.out.println(nameS + ": " + empfangen);

                if (empfangen == null || empfangen.equals("/quit")) {
                    break;
                } else if (empfangen.equals("/list")) {
                    listeBefehle();
                } else if (empfangen.startsWith("/name")) {
                    updateName(empfangen);
                } else if (empfangen.equals("/quiz")) {
                    stelleRechenaufgabe();
                } else if (empfangen.startsWith("/privat")) {
                    privatNachricht(empfangen.substring(8));
                } else {
                    server.sendeAnAlle(this, empfangen);
                }

            } catch (IOException e) {
                System.err.println("Fehler beim Empfangen: Client hat " +
                        "die Verbindung beendet");
                break;
            }
        }

        close();
    }

    public void close() {
        try {
            reader.close();
            writer.close();
            clientSocket.close();
            System.out.println("ClientThread beendet: " + nameS);
            server.sendeAnAndere(this, nameC + " hat den Chat verlassen.");
        } catch (IOException e) {
            System.err.println("Fehler beim Schließen des Sockets");
        }
    }
}