$git = "C:\Program Files\Git\cmd\git.exe"

& $git init
& $git config user.name "gian1475"
& $git config user.email "gian1475@users.noreply.github.com"
& $git add .
& $git commit -m "feat: estructura inicial fullstack (backend spring boot, frontend angular, database postgresql)"
& $git branch -M main

$remotes = & $git remote
if ($remotes -contains "origin") {
    & $git remote set-url origin https://github.com/gian1475/proy_IntegradorFullStack.git
} else {
    & $git remote add origin https://github.com/gian1475/proy_IntegradorFullStack.git
}

& $git push -u origin main --force
