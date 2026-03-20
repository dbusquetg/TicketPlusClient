/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.ticketmaster.ticketplusclient;

import com.ticketmaster.ticketplusclient.gui.LoginGUI;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author Christian
 */
public class TicketPlusClient extends JFrame{

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
