package luis.fluoxetina.chatwebsocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

  @GetMapping({"/", "/chat"})
  public String forwardToSpa() {
    return "forward:/index.html";
  }
}
