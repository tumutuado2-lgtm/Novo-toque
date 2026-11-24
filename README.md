# New Touch

Aplicativo Android com a função **Arraste Contínuo**: um botão flutuante inicia, pausa e encerra um arraste que não levanta o toque da tela (horizontal, vertical ou diagonal).

## Como funciona (e a limitação do Android)

- Usa o **Serviço de Acessibilidade** do Android (`dispatchGesture` + `StrokeDescription.continueStroke`).
- **Sem soltar (vai e volta):** um único toque é mantido; cada trecho continua exatamente de onde o anterior parou, então o toque nunca é levantado. Ao chegar ao fim do trecho ele volta, sem soltar.
- **Mesma direção (reinicia rápido):** o Android não deixa o toque "pular" de volta ao início sem levantar. Esta opção solta e refaz o toque imediatamente, sempre na mesma direção. É a melhor alternativa possível para rolar sempre para o mesmo lado.
- **Limitação:** o Android não permite que um app prolongue o seu dedo real. O toque contínuo é gerado pelo próprio app, na região central da tela.
- O app não lê o conteúdo da tela, apenas envia gestos.

## Instalar o APK (pelo celular)

1. No GitHub, abra o repositório > **Releases** e baixe `New-Touch.apk`.
2. Abra o arquivo e permita instalar de fontes desconhecidas, se o Android pedir.
3. Abra o New Touch > **Permissões** e siga os passos (Acessibilidade e, no Android 13+, "Permitir configurações restritas").
4. Toque em **Ativar Arraste Contínuo**. O botão flutuante aparece.
5. Toque no botão flutuante: ▶ inicia/pausa/retoma, o ícone de ajustes abre o app, ✕ encerra.
6. Use **Testar** para ver o toque real chegando (o contador "Dedo solto" deve ficar em 0 no modo "Sem soltar").

## Gerar o APK pelo GitHub (sem computador)

O arquivo `.github/workflows/build.yml` compila o app a cada alteração enviada ao repositório (aba **Actions**) e publica o APK em **Releases**. Para editar o app, abra qualquer arquivo no GitHub, toque no lápis, altere e faça o commit: o APK novo é gerado sozinho.

## Estrutura

```
app/src/main/java/com/newtouch/app/
  MainActivity.kt          tela inicial
  PermissionsActivity.kt   permissões explicadas
  TestActivity.kt          teste com toques reais
  TraceView.kt             área de teste
  TouchService.kt          serviço de acessibilidade
  DragEngine.kt            lógica do arraste contínuo
  FloatingController.kt    botão flutuante
  Prefs.kt, TouchState.kt, UiUtil.kt
app/src/main/res/          layouts, ícones, cores, animações
```

Android 10 (API 29) ou superior.
