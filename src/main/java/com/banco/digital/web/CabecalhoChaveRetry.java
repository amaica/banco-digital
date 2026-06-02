package com.banco.digital.web;

/**
 * nome do header pra nao processar a mesma transferencia duas vezes se o cliente der retry
 */
public final class CabecalhoChaveRetry {

    public static final String NOME = "X-Chave-Retry";

    private CabecalhoChaveRetry() {
    }
}
