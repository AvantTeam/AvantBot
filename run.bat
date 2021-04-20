@echo off

@rem Keep running the program until successful exit or force exit
:loop

@rem Pull necessary changes
git pull --rebase --no-edit

@rem Build the .jar file
gradlew deploy

@rem Run the .jar file
java --enable-preview -Dbot.token=%AVANT_BOT_TOKEN% -Dbot.creator=%DISCORD_ID% -jar build/libs/AvantBot.jar

@rem If successfully or forcefully exited, stop
set excode = %ERRORLEVEL%
if excode neq 0 goto loop
if excode neq 130 goto loop
