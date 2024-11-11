package com.example.Seis.graus.de.Bacon.Controllers;

import com.example.Seis.graus.de.Bacon.DTOs.ActorsDTO;
import com.example.Seis.graus.de.Bacon.Services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {
    @Autowired
    ConnectionService connectionService;

    @PostMapping
    public List<String> findConnection(@RequestBody ActorsDTO actors) {
        return connectionService.findConnection(actors.actor1(), actors.actor2());
    }
}
