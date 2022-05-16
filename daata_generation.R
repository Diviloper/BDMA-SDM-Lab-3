library(tidyverse)
library(gdata)

dir_path <- "~/Desktop/SDM/LAB_3/BDMA-SDM-Lab-3"
starting_data_path <- paste(dir_path, "/starting_data", sep="")
resources_path <- paste(dir_path, "/src/main/resources/", sep="") # directory of final data


###
# Concepts to have:
# Paper
#   type (fullPaper, ShortPaper, DemoPaper or Poster)
#   title
# Author
#   name
# Pubblication: subset of papers (only the ones with accepted=TRUE)
#   date
#   published_in 
# Venue
#   type (conference or journal)
#   sub_type (type of conference)
# Venue Publication
#   type (volume or proceedings)
#   published_in_volume (volume id)
#   published_in_proceedings (proceedings id)
# Conference
#   names
#   type (Regular, Workshop, Symposium, Expert Group)
# Chair
#   name
# Editor
#   name
# Revision
#   accepted (bool)
#   review_text
# Reviewer
#   name
###

###
# opening data
###

setwd(starting_data_path)

files <- list.files()
for (file in files){
  d <- read_csv(file)
  mv(from = "d", to = strsplit(file, "\\.")[[1]][1])
}



###
# generating publications
# it's done adding a column "published" to papers
###

n_papers <- nrow(papers)
n_published_papers <- (n_papers*0.8) %>% floor
# 80% is published
papers <- papers %>% add_column(`published:bool`=c(rep(1,n_published_papers), 
                                            rep(0, n_papers-n_published_papers)),
                                `type:string`=c(rep(c("full", "short", "demo", "poster"), n_papers/4),"full", "short" )
                                )
papers <- papers %>% select(c(`article:ID`, 
                              `title:string[]`,
                              `published:bool`,
                              `type:string`))


### 
# generating venues
###
conference_types <- c("Regular", "Workshop", "Symposium", "ExpertGroup")
venues <- tibble(`:ID`=c(conferences$`conference:ID`, journals$`:ID`), 
                 `type:string` = c(rep("conference", nrow(conferences)),
                          rep("journal", nrow(journals))),
                 `name:string[]`=c(conferences$`name:string[]`, journals$`journal:string`),
                 `sub_type:string` = c( rep(conference_types, nrow(conferences)/4),
                                        conference_types[1:2],
                                   rep(NA, nrow(journals)))
                 )

###
# revisions
###
revisions <- tibble(`:ID`=1:n_papers, 
                    `accepted:bool`=c( rep(TRUE, n_published_papers),
                                       rep(FALSE, n_papers-n_published_papers)),
                    `text:string[]`=rep("ABC abc", n_papers)
                    )

###
# regarding
###
regarding <- tibble(`:START_ID`=revisions$`:ID`,
                    `:END_ID`=papers$`article:ID`)


###
# reviewers: selected from the authors table
###
reviewers <- authors %>% sample_n(200)

###
# done_by
###
done_by <- tibble(`:START_ID`=rep(revisions$`:ID`,2), # each revision is done by 2 reviewers
                   `:END_ID`=sample(reviewers$`:ID`, nrow(revisions)*2, replace=TRUE))

###
# areas
###
areas <- areas %>% add_column(`:ID`=1:494)

###
# venue_related_to (only to 1 areas)
###
venue_related_to <- tibble(`:START_ID`=venues$`:ID`,
                           `:END_ID`=sample(areas$`:ID`, nrow(venues), replace = TRUE))


###
# venue publication: joining volumes and proceedings
### 

venues_publication <- tibble(`:ID`=c(proceedings$`proceedings:ID`,volumes$`volume:ID`), 
                            `name:string[]`=c(proceedings$`booktitle:string` , volumes$`date:date`),
                            `type` = c(rep("proceedings", nrow(proceedings)),
                                       rep("volume", nrow(volumes)))
                 )

### 
# published_in
###
publications_ids <- papers %>% filter(`published:bool`==TRUE) %>% 
  select(`article:ID`) %>% as_vector
published_in <- tibble(`:START_ID`= publications_ids,
                       `:END_ID`= sample(venues_publication$`:ID`, length(publications_ids), replace=TRUE))
###
# submitted_to
###
submitted_to <- tibble(`:START_ID`= publications_ids,
                       `:END_ID`= sample(venues$`:ID`, length(publications_ids), replace=TRUE))




# all the other tables remain what they are
###


write_csv(papers, paste(resources_path, "paper"))
write_csv(authors, paste(resources_path, "author"))
write_csv(paper_author, paste(resources_path, "submitted_by"))
write_csv(papers_areas, paste(resources_path, "paper_related_to"))
write_csv(areas, paste(resources_path, "area"))
write_csv(revisions, paste(resources_path, "revision"))
write_csv(regarding, paste(resources_path, "regarding"))
write_csv(reviewers, paste(resources_path, "reviewer"))
write_csv(done_by, paste(resources_path, "done_by"))
write_csv(chairs, paste(resources_path, "chair"))
write_csv(editors, paste(resources_path, "editors"))
write_csv(chairs_conferences, paste(resources_path, "manages_conference"))
write_csv(editors_journals, paste(resources_path, "manages_journal"))
write_csv(volumes, paste(resources_path, "volume"))
write_csv(proceedings, paste(resources_path, "proceedings"))
write_csv(`proceedings belongs_to_conference`, paste(resources_path, "belongs_to_conference"))
write_csv(volumes_belongs_to_journals, paste(resources_path, "belongs_to_journal"))
write_csv(venue_related_to, paste(resources_path, "venue_related_to"))
write_csv(published_in, paste(resources_path, "published_in"))
write_csv(submitted_to, paste(resources_path, "submitted_to"))

