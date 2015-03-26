package kangarko.chatcontrol.hooks;

import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SimpleClansHook
{
    private final SimpleClans sc;

    public SimpleClansHook(){
        sc = (SimpleClans) Bukkit.getPluginManager().getPlugin("SimpleClans");
    }

    public String getClanTag(Player p){
        ClanPlayer clanPlayer = sc.getClanManager().getClanPlayer(p);
        if(clanPlayer !=null){
            return clanPlayer.getClan().getColorTag();
        }

        return null;
    }
}
