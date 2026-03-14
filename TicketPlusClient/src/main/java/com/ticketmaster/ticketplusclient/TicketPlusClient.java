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
public class TicketPlusClient extends JFrame{

    private LoginGUI loginGui;

    /**
     * Sobre carga de clase Main con la incializacion del JFrame
     *
     */
    public TicketPlusClient(){
        loginGui = new LoginGUI();
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args){
        
        SwingUtilities.invokeLater(() -> {
            LoginGUI loginGui = new LoginGUI();
            loginGui.setVisible(true);
        });
        
    }
}
