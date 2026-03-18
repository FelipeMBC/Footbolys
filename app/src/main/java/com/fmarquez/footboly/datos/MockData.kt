package com.fmarquez.footboly.datos

import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.Team

fun mockTeams(): MutableList<Team> {
    return mutableListOf(
        Team(
            id = 1,
            name = "Leones FC",
            logoEmoji = "🦁",
            players = mutableListOf(
                Player(1, "Carlos Díaz", 1),
                Player(2, "Matías Soto", 2),
                Player(3, "Javier Rojas", 3),
                Player(4, "Benjamín Lara", 4),
                Player(5, "Tomás Pérez", 5),
                Player(6, "Diego Campos", 6),
                Player(7, "Felipe Muñoz", 7),
                Player(8, "Nicolás Silva", 8),
                Player(9, "Ignacio Reyes", 9),
                Player(10, "Lucas Torres", 10),
                Player(11, "Cristóbal Vega", 11),
                Player(12, "Sebastián Araya", 12),
                Player(13, "Andrés Fuentes", 13),
                Player(14, "Francisco Núñez", 14),
                Player(15, "Vicente Morales", 15),
                Player(16, "Martín Salazar", 16)
            )
        ),
        Team(
            id = 2,
            name = "Tigres United",
            logoEmoji = "🐯",
            players = mutableListOf(
                Player(101, "Pedro León", 1),
                Player(102, "Raúl Castillo", 2),
                Player(103, "Álvaro Parra", 3),
                Player(104, "Bruno Vidal", 4),
                Player(105, "Héctor Ramírez", 5),
                Player(106, "Esteban Cruz", 6),
                Player(107, "Simón Herrera", 7),
                Player(108, "Daniel Pino", 8),
                Player(109, "Marco Bravo", 9),
                Player(110, "Pablo Molina", 10),
                Player(111, "Renato Jara", 11),
                Player(112, "Emilio Vera", 12),
                Player(113, "Gabriel Cea", 13),
                Player(114, "Iván Cisternas", 14),
                Player(115, "Joaquín Flores", 15)
            )
        )
    )
}