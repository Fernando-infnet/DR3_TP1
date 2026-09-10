# TP2 — Respostas discursivas

## 1. Planejamento da aplicação

O projeto se chama MercadoVendas e representa uma loja virtual. Seu objetivo é manter um catálogo de produtos e permitir a criação de pedidos.

O `product-service` cadastra e consulta produtos. O `order-service` cria e consulta pedidos, verificando se o produto existe antes de confirmar o pedido. Cada serviço possui seu próprio banco lógico: `product_db` e `order_db`.

A separação permite que catálogo e pedidos evoluam, sejam implantados e escalados de forma independente.

## 2. Máquina virtual × container

Uma máquina virtual executa um sistema operacional completo sobre um hipervisor. Por isso, normalmente consome mais memória, disco e tempo de inicialização.

Um container compartilha o kernel do sistema operacional do host e contém apenas a aplicação e suas dependências. Ele inicia mais rapidamente e tende a consumir menos recursos.

Containers são úteis para microsserviços porque cada serviço pode ser empacotado, configurado, implantado e escalado de forma independente.

## 6. Componentes Docker

O Dockerfile descreve como gerar a imagem de cada serviço. A imagem é o pacote imutável com a aplicação e suas dependências. O container é uma instância da imagem em execução.

O Docker Engine constrói imagens e executa containers. O port mapping expõe uma porta do container no computador, como `8091:8080`. A network cria uma rede isolada para os containers se comunicarem.

A diferença principal é que a imagem é o molde; o container é a aplicação rodando a partir desse molde.

## 7 e 8. Comunicação e Docker Network

Ao criar um pedido, o `order-service` consulta o `product-service` para verificar se o produto existe. Os dois containers pertencem à rede `marketflow-network`.

O endereço utilizado é `http://product-service:8080`. O Docker Compose resolve `product-service` pelo DNS interno da rede. Assim, não é usado `localhost`, que apontaria para o próprio container do `order-service`, nem IP fixo.

## 10. Docker × Kubernetes

No Kubernetes, o container é executado em um Pod. A rede é fornecida pelo cluster, e os Services oferecem DNS e acesso estável entre aplicações. Um Deployment define a quantidade de réplicas e mantém os Pods ativos.

A aplicação não precisa ser reescrita porque a mesma imagem Docker é utilizada. O Kubernetes apenas define como os containers serão criados, conectados, monitorados e escalados.

## 13. Descoberta de serviço e balanceamento

O Service fornece um endereço estável para acessar um microsserviço e distribui as requisições entre os Pods prontos. Vários Pods do mesmo serviço aumentam a capacidade e a disponibilidade.

Se um Pod falhar, o Deployment tenta recriá-lo. Enquanto ele não estiver pronto, o Service deixa de enviar tráfego para ele. Com três réplicas do `product-service`, as requisições são distribuídas entre as réplicas disponíveis.

## 14. Avaliação e reflexão

A parte mais fácil foi containerizar serviços Spring Boot já independentes. A parte mais difícil foi configurar a comunicação sem usar `localhost`, pois cada container e Pod possui sua própria rede.

O principal aprendizado foi entender que Docker empacota e executa a aplicação, enquanto Kubernetes organiza rede, saúde, réplicas e balanceamento. A autoavaliação de 0 a 10 deve ser preenchida pelo aluno conforme sua experiência.
