#pragma once
#include <set>
#include <map>
#include <filesystem>
#include "../bytes.h"

namespace leveldb {
class DB;
};

class Storage
{
public:
    virtual void Put(const bytes& Key, const bytes& Value) = 0;
    virtual bool Get(const bytes& Key, bytes* Value) = 0;
    virtual bytes Get(const bytes& Key) = 0;
    virtual void Delete(const bytes Key) = 0;
    virtual void Flush();
    void Display();
};

class DummyStorage : public Storage
{
public:
    DummyStorage() = default;
    ~DummyStorage() = default;
    void Put(const bytes& Key, const bytes& Value) override;
    bool Get(const bytes& Key, bytes* Value) override;
    bytes Get(const bytes& Key) override;
    void Delete(const bytes Key) override;
    void Display();
private:
    std::map<bytes, bytes> _Storage;
};

class LevelDB : public Storage
{
public:
    LevelDB(const std::string& db_path);
    ~LevelDB();
    void Put(const bytes& Key, const bytes& Value) override;
    bytes Get(const bytes& Key) override;
    bool Get(const bytes& Key, bytes* Value) override;
    void Delete(const bytes Key) override;
    void Close();
    void Flush() override;
private:
    leveldb::DB* _Db;
};

class StorageWrapper : public Storage
{
public:
    StorageWrapper(Storage* Db, const bytes& Prefix = "");
    ~StorageWrapper();
    void Put(const bytes& Key, const bytes& Value) override;
    bytes Get(const bytes& Key) override;
    bool Get(const bytes& Key, bytes* Value) override;
    void Delete(const bytes Key) override;
    void Display();
    void Flush() override;
private:
    Storage* _Db;
    bytes _Prefix;
};

class StorageCacheWrapper : public Storage
{
public:
    StorageCacheWrapper(Storage* Db);
    ~StorageCacheWrapper();
    void Put(const bytes& Key, const bytes& Value) override;
    bytes Get(const bytes& Key) override;
    bool Get(const bytes& Key, bytes* Value) override;
    void Delete(const bytes Key) override;
    void Flush() override;
    void Display();
private:
    Storage* _Db;
    std::map<bytes, bytes> _Cache;
    std::set<bytes> _Deletes;
};