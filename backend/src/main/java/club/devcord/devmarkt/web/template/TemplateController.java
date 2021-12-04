/*
 * Copyright 2021 Contributors to the Devmarkt-Backend project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package club.devcord.devmarkt.web.template;

import club.devcord.devmarkt.dto.Identified;
import club.devcord.devmarkt.dto.template.Template;
import club.devcord.devmarkt.dto.template.TemplateEvent;
import club.devcord.devmarkt.services.template.TemplateService;
import club.devcord.devmarkt.util.BaseUriBuilder;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.sse.Event;
import java.util.List;
import org.reactivestreams.Publisher;

@Controller("/template")
public class TemplateController {
  private final TemplateService service;

  public TemplateController(TemplateService templateService) {
    this.service = templateService;
  }

  @Get("/events")
  @Produces(MediaType.TEXT_EVENT_STREAM)
  public Publisher<Event<TemplateEvent>> events() {
    return service.subscribeEvents();
  }

  @Post
  @CreateSwagger
  @Status(HttpStatus.CREATED)
  public HttpResponse<Void> createTemplate(@Body Identified<Template> body) {
    var template = body.value();
    var requesterID = body.requesterID();
    return switch (service.create(template, requesterID)) {
      case ERROR -> HttpResponse.serverError();
      case DUPLICATED -> HttpResponse.status(HttpStatus.CONFLICT);
      case CREATED -> HttpResponse.created(BaseUriBuilder.of("template", template.name()));
    };
  }

  @Put
  @ReplaceSwagger
  @Status(HttpStatus.NO_CONTENT)
  public HttpResponse<Void> replaceTemplate(@Body Identified<Template> body) {
    var template = body.value();
    var requesterID = body.requesterID();
    return switch (service.replace(template, requesterID)) {
      case ERROR -> HttpResponse.serverError();
      case NOT_MODIFIED -> HttpResponse.notModified();
      case NOT_FOUND -> HttpResponse.notFound();
      case REPLACED -> HttpResponse.noContent()
          .header(HttpHeaders.LOCATION, BaseUriBuilder.of("template", template.name()).toString())
          .body(null);
    };
  }

  @Get("/{name}")
  @GetSwagger
  public HttpResponse<Template> getTemplate(@PathVariable String name) {
    var result = service.get(name);
    return result.isEmpty()
        ? HttpResponse.notFound()
        : HttpResponse.ok(result.get());
  }


  @Get
  @ListSwagger
  public HttpResponse<List<String>> getListOfNames() {
    return HttpResponse.ok(service.names());
  }

  @Delete(value = "/{name}")
  @DeleteSwagger
  @Status(HttpStatus.NO_CONTENT)
  public HttpResponse<Void> delete(@PathVariable String name, @Body String requesterID) {
    return switch (service.delete(name, requesterID)) {
      case ERROR -> HttpResponse.serverError();
      case NOT_FOUND -> HttpResponse.notFound();
      case DELETED -> HttpResponse.noContent();
    };
  }
}
