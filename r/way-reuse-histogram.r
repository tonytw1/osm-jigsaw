library(dplyr)

way_usages <- read.csv('area-ways.csv')

reuse <- way_usages  %>% group_by(WAY) %>% summarise(count = n())

h <- hist(reuse$count, ,breaks=seq(1,50,1))
h$counts

h <- hist(reuse$count, ,breaks=c(1,2,50))
h$counts

library(ggplot2)
qplot(reuse$count, geom='histogram', breaks=seq(1,50,1), xlim=c(0,10), main = "Way reuse", xlab="Usages", ylab="Count")
ggplot(data = reuse, aes(reuse$count)) + geom_histogram(breaks=seq(1,50,1))
ggsave("out.png", width=12, height=8, units="cm", dpi=150)

