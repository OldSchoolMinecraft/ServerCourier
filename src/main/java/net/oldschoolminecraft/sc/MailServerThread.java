package net.oldschoolminecraft.sc;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.OfflinePlayer;
import com.earth2me.essentials.User;
import com.google.gson.Gson;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MailServerThread extends Thread
{
    private static final Gson gson = new Gson();

    private final int port;
    public volatile boolean running = true;

    public MailServerThread(int port)
    {
        this.port = port;
    }

    @Override
    public void run()
    {
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            while (running)
            {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(() -> handleClientRequest(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientRequest(Socket clientSocket)
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true))
        {
            String message = reader.readLine();
            String response;
            if (message != null)
            {
                MCMail mail = gson.fromJson(message, MCMail.class);
                if (mail.isValidMessage())
                {
                    User user = ServerCourier.essentials.getOfflineUser(mail.recipient);
                    if (user != null)
                    {
                        user.addMail(mail.sender + ": " + mail.message);
                        response = "OK";
                    } else response = "Recipient could not be found.";
                } else response = "Mail data is invalid";
            } else response = "Message is empty or has an invalid length";
            writer.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try
            {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
