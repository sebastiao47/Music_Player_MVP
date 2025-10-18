# Player Seba - Leitor de Música para Android

Player Seba é uma aplicação de leitor de música para Android, desenvolvida como um projecto de demonstração de competências em desenvolvimento de software. A aplicação permite aos usuários gerir e ouvir as suas músicas locais, criar playlists, e até mesmo identificar músicas que estão a tocar no ambiente, utilizando a API da ACRCloud.

## Funcionalidades

O projecto implementa um conjunto completo de funcionalidades esperadas de um leitor de música moderno:

- **Reprodução de Música:** Controlo total sobre a reprodução com botões de Play, Pause, Próxima e Anterior.
- **Controlo Avançado:** Funcionalidades de Shuffle (ordem aleatória) e Repeat (repetir uma música ou a lista inteira).
- **Barra de Progresso (SeekBar):** Visualização em tempo real do progresso da música e controlo para avançar ou retroceder.
- **Serviço em Background:** A música continua a tocar mesmo que a aplicação seja fechada ou o ecrã bloqueado, graças a um `Service` robusto.
- **Notificação de Mídia:** Controlo da reprodução directamente a partir da barra de notificações do Android.
- **Gestão de Playlists:**
    - Criar e apagar playlists.
    - Adicionar músicas da biblioteca a qualquer playlist.
    - Visualizar as músicas dentro de uma playlist e remover músicas específicas.
    - Ver a contagem de músicas em cada playlist.
- **Identificação de Música:** Integração com a API da **ACRCloud** para reconhecer músicas que estão a tocar no ambiente.
- **Busca:** Pesquisa rápida por músicas na biblioteca ou na lista de reprodução actual.
- **Detalhes da Música:** Opção para ver os metadados da música que está a tocar.

## Arquitectura e Tecnologias

O projecto foi desenvolvido seguindo o padrão de arquitectura **MVP (Model-View-Presenter)** para garantir uma separação clara de responsabilidades e facilitar a manutenção do código.

- **Linguagem:** Java
- **Arquitectura:** MVP (Model-View-Presenter)
- **Base de Dados:** SQLite, gerido através da biblioteca **Room Persistence Library**.
- **Componentes de UI:** `RecyclerView`, `Toolbar`, `SeekBar`, `FloatingActionButton`.
- **Threading:** `ExecutorService` para operações de I/O em background (base de dados).
- **Serviços:** `Service` para reprodução de música em background e `BroadcastReceiver` para os controlos da notificação.
- **APIs Externas:** ACRCloud SDK para o reconhecimento de áudio.

## Como Compilar e Gerar a APK

1.  **Clonar o Repositório:**
    ```bash
    git clone [URL_DO_SEU_REPOSITORIO]
    ```
2.  **Configurar a Chave da ACRCloud:**
    - Abra o ficheiro `LivrariaActivity.java`.
    - Encontre o método `initializeAndStartRecognition()`.
    - Substitua os valores de `config.accessKey` e `config.accessSecret` pelas suas próprias credenciais da ACRCloud.
3.  **Sincronizar o Projecto:** Abra o projecto no Android Studio e espere o Gradle sincronizar as dependências.
4.  **Gerar a APK Assinada:**
    - No menu, vá para **Build > Generate Signed Bundle / APK...**.
    - Seleccione **APK** e clique em **Next**.
    - Crie uma nova chave de assinatura (`keystore`) ou use uma existente.
    - Escolha a variante de build **release**.
    - Clique em **Create**.
    - A APK assinada (`app-release.apk`) estará localizada na pasta `app/release/`.

## Dificuldades e Soluções

O desenvolvimento enfrentou desafios comuns em aplicações de mídia para Android, como a gestão do ciclo de vida dos componentes e a comunicação com serviços em background. A solução para os crashes relacionados com a notificação (`Bad notification for startForeground`) envolveu mover a criação do canal de notificação para dentro do próprio serviço, garantindo que ele sempre exista antes de ser usado. Problemas de actualização da UI foram resolvidos ao mover a lógica de carregamento de dados para o método `onResume()`, garantindo que as telas sempre mostrem a informação mais recente.
