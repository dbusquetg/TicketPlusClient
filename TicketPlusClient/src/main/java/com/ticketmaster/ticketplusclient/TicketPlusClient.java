/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.ticketmaster.ticketplusclient;

import com.ticketmaster.ticketplusclient.gui.LoginGUI;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

/**
 *
 * @author Christian
 */
public class TicketPlusClient extends JFrame implements CommandLineRunner {

    private LoginGUI loginGui;

    /**
     * Sobre carga de clase Main con la incializacion del JFrame
     *
     */
    public TicketPlusClient(){
        loginGui = new LoginGUI();
    }

    /**
     * Metodo static main que invoco SpringApplication.run
     *
     * @param args
     */
    public static void main(String[] args){
        /**
         * Se pasa la propia clase TicketPlusClient.class como argumentos
         */
        SpringApplication.run(TicketPlusClient.class, args);
    }
    /**
     *Metodo run invocado por SpringApplication
     *
     * @param args
     * @throws Exception
     */
    public void run(String... args) throws Exception {
        SwingUtilities.invokeLater(() -> loginGui.setVisible(true));
    }
}
