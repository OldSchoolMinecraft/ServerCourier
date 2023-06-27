package net.oldschoolminecraft.sc;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.google.gson.Gson;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xbill.DNS.*;

import java.io.*;
import java.net.Socket;

public class ServerCourier extends JavaPlugin
{
    private static final Gson gson = new Gson();
    public static Essentials essentials;
    private CourierConfig config;
    private MailServerThread mailServerThread;

    public void onEnable()
    {
        essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        config = new CourierConfig(new File(getDataFolder(), "config.yml"));
        mailServerThread = new MailServerThread((int) config.getConfigOption("listen_port"));
        mailServerThread.start();

        System.out.println("ServerCourier enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (label.equalsIgnoreCase("mail"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
                return true;
            }

            if (args.length < 1)
            {
                sender.sendMessage(ChatColor.RED + "Insufficient arguments. Usage: /mail <send/read>");
                return true;
            }

            if (args[0].equalsIgnoreCase("read"))
            {
                User user = essentials.getOfflineUser(sender.getName());
                user.getMails().forEach((mail) ->
                {
                    String[] parts = mail.split(":");
                    String from = parts[0];
                    String message = parts[1].trim();
                    sender.sendMessage(ChatColor.DARK_GRAY + "From <" + from + ">: " + '"' + ChatColor.GRAY + message + '"' );
                });
            }

            if (args[0].equalsIgnoreCase("send"))
            {
                if (args.length < 3)
                {
                    sender.sendMessage(ChatColor.RED + "Insufficient arguments. Usage: /mail send <target[@host]> <message>");
                    return true;
                }

                String username = args[1];
                String message = catStrings(args, 2);

                if (username.contains("@"))
                {
                    String target = username.split("@")[0];
                    String host = username.split("@")[0];

                    String response = sendMail(sender.getName(), target, message, host);
                    if (!response.equalsIgnoreCase("OK"))
                    {
                        sender.sendMessage(ChatColor.RED + "Mail delivery error: " + response);
                    } else sender.sendMessage(ChatColor.GREEN + "Mail was (remotely) delivered successfully!");
                } else {
                    User user = essentials.getOfflineUser(username);
                    user.addMail(sender.getName() + ": " + message);
                    sender.sendMessage(ChatColor.GREEN + "Mail was (locally) delivered successfully!");
                }
            }
        }

        return false;
    }

    public String catStrings(String[] strings, int startIndex)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < strings.length; i++)
            sb.append(strings[i]).append(" ");
        return sb.toString().trim();
    }

    public String sendMail(String sender, String recipient, String message, String serverHost)
    {
        int serverPort = getPreferredPort(serverHost);
        try (Socket socket = new Socket(serverHost, serverPort); PrintWriter writer = new PrintWriter(socket.getOutputStream(), true))
        {
            MCMail mail = new MCMail(sender, recipient, message);
            String json = gson.toJson(mail);
            writer.println(json);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public int getPreferredPort(String host)
    {
        try
        {
            Lookup lookup = new Lookup(host, Type.SRV);
            Record[] records = lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL)
                return records.length > 0 ? ((SRVRecord) records[0]).getPort() : 26553;
            else return 26553;
        } catch (TextParseException e) {
            e.printStackTrace();
            return 26553;
        }
    }

    public void onDisable()
    {
        mailServerThread.running = false;
        mailServerThread.interrupt();

        try
        {
            mailServerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("ServerCourier disabled");
    }
}
