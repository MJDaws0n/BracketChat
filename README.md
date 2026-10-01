# BracketChat — Paper 26.2

Normal chat becomes a server/system message: `[MJDawson] hello`.
The original chat event is cancelled, so the plugin sends one copy to each online player across all worlds and one to console.

## Install

Upgrading from an earlier version: stop the server, remove the old BracketChat JAR, copy in 1.0.2, then start the server. Your existing config can stay; whispers are private even if `broadcast-private-messages` is still true.

1. Run Paper 26.2 with Java 25.
2. Copy `BracketChat-1.0.2.jar` into the server's `plugins` directory.
3. Restart the server (do not use `/reload`). No client mods or additional plugins are required.

## Commands

| Existing command | Explicit replacement | Default delivery |
| --- | --- | --- |
| Normal chat | `/global <message>` or `/g <message>` | Everyone |
| `/say <message>` | `/bsay <message>` | Everyone; operator-only |
| `/w`, `/msg`, `/tell <player> <message>` | `/bmsg <player> <message>` | Selected player(s) and sender only |
| `/me <action>` | `/bme <action>` | Everyone |
| `/teammsg`, `/tm <message>` | `/bteammsg <message>` | Everyone; sender must belong to a team |

Whispers (`/w`, `/msg`, `/tell`, `/bmsg`) use `(W)[username] message`, with the entire line grey and italic for both sender and recipient. Other chat uses `[username] message`. `/me` deliberately has no asterisk prefix.
Whisper commands still require a valid online target; team commands still require a vanilla scoreboard team.
Targets can be exact online player names or Minecraft selectors; selectors require `minecraft.command.selector`.
Message bodies are literal text: colour markup and selectors in the message itself are not expanded.

## Public versus private

**Whispers are always private**: `/w`, `/msg`, `/tell` and `/bmsg` send only to the selected player(s) plus the sender, regardless of existing config settings. Sending to yourself produces one copy.
Team messages remain public by default. To restrict team messages to teammates, edit
`plugins/BracketChat/config.yml`:

```yaml
broadcast-private-messages: false
```

Restart after editing. This legacy-named setting now controls only team messages: when false, `/tm` sends only to online members of the sender's main-scoreboard team. Private messages are not explicitly copied to console by this plugin (server command logging/other plugins may still log them).

## Permissions

- `bracketchat.chat`: normal chat, `/global`, `/g` (everyone by default).
- `bracketchat.whisper`: `/w`, `/msg`, `/tell`, `/bmsg` (everyone).
- `bracketchat.me`: `/me`, `/bme` (everyone).
- `bracketchat.team`: `/teammsg`, `/tm`, `/bteammsg` (everyone).
- `bracketchat.say`: `/say`, `/bsay` (operators by default).

## Scope and compatibility

Player, console and RCON commands are redirected, including direct `minecraft:` and `bukkit:` forms.
Explicit `/bracketchat:bmsg`, `/bracketchat:bsay`, etc. are available if another plugin owns a short alias.
The plugin respects chat/command events already cancelled by another plugin. Use it as the server's chat formatter; other chat plugins can interfere. Command replacements do not fire a normal player chat event, so chat-only mute/filter plugins need their own command restrictions.

`/tellraw` is deliberately unchanged: it sends arbitrary structured text to selected recipients and has no built-in username wrapper to replace. `/title` and `/bossbar` are also unchanged because they are not chat messages. `/reply`, `/r`, `/broadcast` and `/whisper` are not vanilla Java command aliases and are not intercepted.

Commands nested in `/execute`, functions, command blocks, or invoked directly by another plugin are not rewritten by the player/console preprocess hooks. Use the explicit `bracketchat:` replacement commands there. Third-party namespaced commands (such as `/essentials:msg`) remain owned by that plugin.

System messages use a different client delivery path from signed player chat. This does not override a player's Hidden chat setting or account/parental restrictions. This is a Paper plugin, not a Folia plugin.

## Build

Install JDK 25 and Maven 3.9+, then run:

```sh
mvn clean package
```

Output: `target/BracketChat-1.0.2.jar`. Paper API is provided by the server, not bundled.
API dependency is pinned to `26.2.build.129-stable`.

## References

- https://docs.papermc.io/paper/dev/project-setup/
- https://docs.papermc.io/paper/dev/plugin-yml/
- https://docs.papermc.io/paper/dev/chat-events/
- https://jd.papermc.io/paper/26.2/io/papermc/paper/event/player/AsyncChatEvent.html

## Verification

Build and automated-test results are included in `BUILD-RESULT.txt` in the download bundle.
A real multiplayer server/client session was not available for end-to-end verification.
After installing, join with two accounts and check normal chat, each command alias, permissions, missing targets, and private mode if enabled.

## Downloads and GitHub Packages

The latest plugin is **1.0.2**. Releases include the original compiled JAR, exact source ZIP and SHA-256 checksums. Version 1.0.0 is retained for history and broadcasts whispers by default; prefer 1.0.2.

The repository includes all three original versions under `releases/`. The manually run **Publish releases and Maven packages** workflow publishes them to GitHub Releases and GitHub Packages. Run it after uploading the repository; it uses GitHub's automatic workflow token, with no personal access token required for publishing.

Maven coordinates: `net.mjdawson:bracket-chat:1.0.2`.
Registry: `https://maven.pkg.github.com/mjdaws0n/bracketchat`.
For installation on a Minecraft server, simply download the JAR from Releases.
