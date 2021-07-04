package bot.core;

import net.dv8tion.jda.api.entities.*;

import java.util.*;

import org.slf4j.*;

import static bot.AvantBot.*;

@SuppressWarnings("unchecked")
public class Warns {
    private static final Logger LOG = LoggerFactory.getLogger(Warns.class);

    public Warns() {
        LOG.debug("Initialized member warnings utility.");
    }

    public void warn(Message message, Member member, String reason) {
        var map = data(member, false);

        var count = (int)map.getOrDefault("count", 0) + 1;
        map.put("count", count);

        var reasons = (List<String>)map.getOrDefault("reasons", new ArrayList<String>());
        reasons.add(reason == null ? "Unspecified" : reason);
        map.put("reasons", reasons);

        settings.save();
        LOG.info("{}#{} now has {} warning{}.", member.getEffectiveName(), member.getUser().getDiscriminator(), count, count == 1 ? "" : "s");

        if(message != null) {
            message.getTextChannel()
                .sendMessage(String.format("%s, you have been warned **%s**%s",
                    member.getAsMention(),
                    messages.warnMessage(count),
                    reason == null
                    ?   "."
                    :   String.format(" with a reason: **%s**", reason)
                ))
                .queue();
        }
    }

    public void clearwarn(Message message, Member member) {
        var map = data(member, false);

        map.put("count", 0);
        ((List<String>)map.get("reasons")).clear();

        settings.save();
        LOG.info("{}#{}'s warnings have been cleared.", member.getEffectiveName(), member.getUser().getDiscriminator());

        if(message != null) {
            message.getTextChannel()
                .sendMessage(String.format("%s, your warnings have been cleared.", member.getAsMention()))
                .queue();
        }
    }

    public List<String> warnings(Member member) {
        return (List<String>)data(member, true).get("reasons");
    }

    private Map<String, Object> data(Member member, boolean save) {
        Map<String, Map<String, Object>> warns = settings.get("warns", Map.class);

        Map<String, Object> map = warns.getOrDefault(member.getId(), new LinkedHashMap<>());
        warns.put(member.getId(), map);

        var count = (int)map.getOrDefault("count", 0);
        map.put("count", count);

        var reasons = (List<String>)map.getOrDefault("reasons", new ArrayList<String>());
        map.put("reasons", reasons);

        if(save) settings.save();
        return map;
    }
}
