/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.ticketmaster.ticketplusclient;

import com.ticketmaster.ticketplusclient.gui.LoginGUI;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Clase pricipal de la aplicacion de escritorio TicketPlus.
 * 
 * <p>Punto de entrada de la apliacion. Incializa la interfaz grafica lanzando la
 * ventana de login en el hilo de eventos de Swing (EDT) mediante 
 * {@link SwingUtilities#invokeLater(Runnable)}</p> 
 * 
 * @author Christian
 */
public class TicketPlusClient extends JFrame{

    /**
     * Método principal de la aplicación. Lanza la ventana de login en el
     * Event Dispatch Thread (EDT) de Swing para asegurar la correcta
     * inicialización de la interfaz gráfica.
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
